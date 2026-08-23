package com.flowforge.cache;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flowforge.domain.FulfillmentNetwork;
import com.flowforge.domain.NetworkNode;
import com.flowforge.engine.graph.TimeDependentGraph;
import com.flowforge.engine.routing.PathSearch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Redis-backed all-pairs path labels with an in-process fallback.
 */
public class PrecomputeCache {

    private static final Logger log = LoggerFactory.getLogger(PrecomputeCache.class);

    private final StringRedisTemplate redis;
    private final ObjectMapper mapper;
    private final boolean redisEnabled;
    private final String prefix;
    private final Duration ttl;
    private final Map<String, Double> local = new ConcurrentHashMap<>();

    public PrecomputeCache(
            StringRedisTemplate redis,
            ObjectMapper mapper,
            boolean redisEnabled,
            String prefix,
            long ttlSeconds
    ) {
        this.redis = redis;
        this.mapper = mapper;
        this.redisEnabled = redisEnabled && redis != null;
        this.prefix = prefix;
        this.ttl = Duration.ofSeconds(ttlSeconds);
    }

    public void warm(FulfillmentNetwork network, TimeDependentGraph graph, int slaHours) {
        int labels = 0;
        for (NetworkNode node : network.fulfillmentNodes()) {
            for (var region : network.regions()) {
                for (int hour = 0; hour < 24; hour += 3) {
                    PathSearch.PathResult path = PathSearch.astar(
                            graph, network, node.id(), region.id(), hour, slaHours
                    );
                    put(key(node.id(), region.id(), hour), path.found() ? path.cost() : -1.0);
                    labels++;
                }
            }
        }
        log.info("Warmed {} routing labels (redis={})", labels, redisEnabled);
    }

    public Double get(String source, String region, int hour) {
        String cacheKey = key(source, region, hour);
        Double localHit = local.get(cacheKey);
        if (localHit != null) {
            return localHit < 0 ? null : localHit;
        }
        if (!redisEnabled) {
            return null;
        }
        try {
            String raw = redis.opsForValue().get(cacheKey);
            if (raw == null) {
                return null;
            }
            Double value = mapper.readValue(raw, new TypeReference<>() {
            });
            local.put(cacheKey, value);
            return value < 0 ? null : value;
        } catch (Exception ex) {
            log.debug("Redis read missed: {}", ex.getMessage());
            return null;
        }
    }

    public void put(String cacheKey, double value) {
        local.put(cacheKey, value);
        if (!redisEnabled) {
            return;
        }
        try {
            redis.opsForValue().set(cacheKey, mapper.writeValueAsString(value), ttl);
        } catch (Exception ex) {
            log.debug("Redis write skipped: {}", ex.getMessage());
        }
    }

    private String key(String source, String region, int hour) {
        return prefix + "path:" + source + ":" + region + ":" + hour;
    }
}
