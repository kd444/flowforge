package com.flowforge.service;

import com.flowforge.cache.PrecomputeCache;
import com.flowforge.config.FlowForgeProperties;
import com.flowforge.domain.DemandRegion;
import com.flowforge.domain.FulfillmentNetwork;
import com.flowforge.domain.NetworkNode;
import com.flowforge.domain.ScottishNetworkFactory;
import com.flowforge.engine.forecast.DemandForecaster;
import com.flowforge.engine.forecast.M5Series;
import com.flowforge.engine.forecast.M5SyntheticGenerator;
import com.flowforge.engine.graph.TimeDependentGraph;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

@Component
public class NetworkRuntime {

    private final FlowForgeProperties properties;
    private final PrecomputeCache cache;
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    private FulfillmentNetwork network;
    private List<M5Series> catalog;
    private Map<String, Double> regionForecast;
    private TimeDependentGraph graph;

    public NetworkRuntime(FlowForgeProperties properties, PrecomputeCache cache) {
        this.properties = properties;
        this.cache = cache;
    }

    @PostConstruct
    public void init() {
        lock.writeLock().lock();
        try {
            network = ScottishNetworkFactory.build();
            List<String> stores = network.fulfillmentNodes().stream().map(NetworkNode::id).toList();
            catalog = M5SyntheticGenerator.generate(stores, properties.getForecast().getHistoryDays(), 2026L);
            rebuildForecastAndGraph();
            cache.warm(network, graph, properties.getNetwork().getSlaHoursDefault());
        } finally {
            lock.writeLock().unlock();
        }
    }

    public FulfillmentNetwork network() {
        lock.readLock().lock();
        try {
            return network;
        } finally {
            lock.readLock().unlock();
        }
    }

    public TimeDependentGraph graph() {
        lock.readLock().lock();
        try {
            return graph;
        } finally {
            lock.readLock().unlock();
        }
    }

    public Map<String, Double> forecast() {
        lock.readLock().lock();
        try {
            return regionForecast;
        } finally {
            lock.readLock().unlock();
        }
    }

    public List<M5Series> catalog() {
        return catalog;
    }

    public void replace(FulfillmentNetwork next) {
        lock.writeLock().lock();
        try {
            this.network = next;
            rebuildForecastAndGraph();
        } finally {
            lock.writeLock().unlock();
        }
    }

    public Map<String, Object> snapshot() {
        FulfillmentNetwork live = network();
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("nodes", live.nodes().stream().map(n -> Map.of(
                "id", n.id(),
                "name", n.name(),
                "type", n.type().name(),
                "lat", n.location().lat(),
                "lon", n.location().lon(),
                "capacity", n.storageCapacity(),
                "safetyStock", n.safetyStock(),
                "onHand", n.onHand(),
                "holdingCost", n.holdingCostPerUnit()
        )).toList());
        body.put("regions", live.regions().stream().map(r -> Map.of(
                "id", r.id(),
                "name", r.name(),
                "lat", r.location().lat(),
                "lon", r.location().lon(),
                "assignedNodeId", r.assignedNodeId(),
                "demandScale", r.demandScale(),
                "forecast", regionForecast.getOrDefault(r.id(), 0.0)
        )).toList());
        int onHand = live.fulfillmentNodes().stream().mapToInt(NetworkNode::onHand).sum();
        int capacity = live.fulfillmentNodes().stream().mapToInt(NetworkNode::storageCapacity).sum();
        double forecastUnits = regionForecast.values().stream().mapToDouble(Double::doubleValue).sum();
        body.put("laneCount", live.lanes().size());
        body.put("fulfillmentNodes", live.fulfillmentNodes().size());
        body.put("demandRegions", live.regions().size());
        body.put("data", Map.of(
                "source", "M5 retail demand schema",
                "historyDays", properties.getForecast().getHistoryDays(),
                "skuCount", catalog.size(),
                "horizonDays", properties.getForecast().getHorizonDays(),
                "onHandUnits", onHand,
                "storageCapacity", capacity,
                "dailyForecastUnits", Math.round(forecastUnits * 10.0) / 10.0,
                "forecastMethod", "seasonal-naive + Croston"
        ));
        return body;
    }

    private void rebuildForecastAndGraph() {
        Map<String, Double> scale = network.regions().stream()
                .collect(Collectors.toMap(DemandRegion::id, DemandRegion::demandScale, (a, b) -> a, LinkedHashMap::new));
        regionForecast = DemandForecaster.regionDailyMean(catalog, scale, 56);
        graph = new TimeDependentGraph(network, regionForecast);
    }
}
