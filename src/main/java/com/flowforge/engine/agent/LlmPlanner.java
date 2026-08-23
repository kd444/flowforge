package com.flowforge.engine.agent;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flowforge.domain.FulfillmentNetwork;
import com.flowforge.engine.simulate.SimulationResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

public final class LlmPlanner {

    private static final Logger log = LoggerFactory.getLogger(LlmPlanner.class);

    private final String apiKey;
    private final String baseUrl;
    private final String model;
    private final ObjectMapper mapper;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(8)).build();

    public LlmPlanner(String apiKey, String baseUrl, String model, ObjectMapper mapper) {
        this.apiKey = apiKey;
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.model = model;
        this.mapper = mapper;
    }

    public boolean enabled() {
        return apiKey != null && !apiKey.isBlank();
    }

    public NetworkProposal propose(FulfillmentNetwork network, SimulationResult baseline) {
        if (!enabled()) {
            return HeuristicPlanner.propose(network, baseline);
        }
        try {
            String body = mapper.writeValueAsString(Map.of(
                    "model", model,
                    "temperature", 0.2,
                    "response_format", Map.of("type", "json_object"),
                    "messages", new Object[]{
                            Map.of("role", "system", "content", systemPrompt()),
                            Map.of("role", "user", "content", userPrompt(network, baseline))
                    }
            ));
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/chat/completions"))
                    .timeout(Duration.ofSeconds(20))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 300) {
                log.warn("LLM planner HTTP {}: {}", response.statusCode(), response.body());
                return HeuristicPlanner.propose(network, baseline);
            }
            JsonNode root = mapper.readTree(response.body());
            String content = root.path("choices").path(0).path("message").path("content").asText();
            return mapper.readValue(content, NetworkProposal.class);
        } catch (Exception ex) {
            log.warn("LLM planner falling back to heuristic: {}", ex.getMessage());
            return HeuristicPlanner.propose(network, baseline);
        }
    }

    private static String systemPrompt() {
        return """
                You are the FlowForge network planner. Propose conservative configuration
                changes as JSON with keys rationale, safetyStockDeltas, laneCapacityDeltas,
                assignments. Only use existing node, region, and lane ids. Keep deltas small.
                """;
    }

    private String userPrompt(FulfillmentNetwork network, SimulationResult baseline) throws Exception {
        Map<String, Object> snapshot = new HashMap<>();
        snapshot.put("fillRate", baseline.fillRate());
        snapshot.put("totalCost", baseline.totalCost());
        snapshot.put("unmetUnits", baseline.unmetUnits());
        snapshot.put("nodes", network.fulfillmentNodes().stream().map(n -> Map.of(
                "id", n.id(),
                "onHand", n.onHand(),
                "safetyStock", n.safetyStock(),
                "capacity", n.storageCapacity()
        )).toList());
        snapshot.put("regions", network.regions().stream().limit(60).map(r -> Map.of(
                "id", r.id(),
                "name", r.name(),
                "assigned", r.assignedNodeId()
        )).toList());
        return "Current network snapshot:\n" + mapper.writeValueAsString(snapshot);
    }
}
