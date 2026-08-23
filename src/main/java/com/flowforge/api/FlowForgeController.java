package com.flowforge.api;

import com.flowforge.api.dto.RouteRequestBody;
import com.flowforge.api.dto.SimulateRequestBody;
import com.flowforge.config.FlowForgeProperties;
import com.flowforge.domain.FulfillmentNetwork;
import com.flowforge.engine.agent.HeuristicPlanner;
import com.flowforge.engine.agent.LlmPlanner;
import com.flowforge.engine.agent.NetworkProposal;
import com.flowforge.engine.agent.ProposalVerifier;
import com.flowforge.engine.forecast.DemandForecaster;
import com.flowforge.engine.forecast.M5Series;
import com.flowforge.engine.placement.InventoryPlacer;
import com.flowforge.engine.placement.PlacementPlan;
import com.flowforge.engine.routing.RouteDecision;
import com.flowforge.engine.routing.RoutingEngine;
import com.flowforge.engine.simulate.NetworkSimulator;
import com.flowforge.engine.simulate.SimulationResult;
import com.flowforge.service.NetworkRuntime;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
public class FlowForgeController {

    private final NetworkRuntime runtime;
    private final FlowForgeProperties properties;
    private final LlmPlanner llmPlanner;
    private final ProposalVerifier verifier;

    public FlowForgeController(
            NetworkRuntime runtime,
            FlowForgeProperties properties,
            LlmPlanner llmPlanner,
            ProposalVerifier verifier
    ) {
        this.runtime = runtime;
        this.properties = properties;
        this.llmPlanner = llmPlanner;
        this.verifier = verifier;
    }

    @GetMapping("/network")
    public Map<String, Object> network() {
        return runtime.snapshot();
    }

    @PostMapping("/route")
    public RouteDecision route(@Valid @RequestBody RouteRequestBody body) {
        RoutingEngine.Algorithm algorithm;
        try {
            algorithm = RoutingEngine.Algorithm.valueOf(body.getAlgorithm().trim().toUpperCase());
        } catch (Exception ex) {
            algorithm = RoutingEngine.Algorithm.AUTO;
        }
        return RoutingEngine.route(
                runtime.network(),
                runtime.graph(),
                new RoutingEngine.Request(
                        body.getRegionId(),
                        body.getDepartHour() == null ? 9 : body.getDepartHour(),
                        body.getSlaHours() == null ? properties.getNetwork().getSlaHoursDefault() : body.getSlaHours(),
                        algorithm,
                        body.isRequireStock()
                )
        );
    }

    @PostMapping("/placement")
    public PlacementPlan placement(
            @RequestParam(defaultValue = "9") int departHour,
            @RequestParam(defaultValue = "24") int slaHours
    ) {
        return InventoryPlacer.place(
                runtime.network(),
                runtime.graph(),
                runtime.forecast(),
                departHour,
                slaHours
        );
    }

    @PostMapping("/simulate")
    public Map<String, Object> simulate(@RequestBody(required = false) SimulateRequestBody body) {
        if (body == null) {
            body = new SimulateRequestBody();
        }
        int days = body.getDays() == null ? properties.getSimulation().getDefaultDays() : body.getDays();
        long seed = body.getSeed() == null ? properties.getSimulation().getSeed() : body.getSeed();
        SimulationResult flowforge = NetworkSimulator.run(
                runtime.network(), runtime.graph(), runtime.forecast(),
                NetworkSimulator.Policy.FLOWFORGE, days, seed
        );
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("flowforge", flowforge);
        if (body.isCompareBaseline()) {
            SimulationResult greedy = NetworkSimulator.run(
                    runtime.network(), runtime.graph(), runtime.forecast(),
                    NetworkSimulator.Policy.GREEDY, days, seed
            );
            NetworkSimulator.Comparison comparison = NetworkSimulator.compare(flowforge, greedy);
            response.put("greedy", greedy);
            response.put("costReduction", comparison.costReduction());
            response.put("fillRateDelta", comparison.fillRateDelta());
        }
        return response;
    }

    @PostMapping("/agent/tick")
    public Map<String, Object> agentTick(
            @RequestParam(defaultValue = "false") boolean applyIfAccepted,
            @RequestBody(required = false) NetworkProposal override
    ) {
        FulfillmentNetwork live = runtime.network();
        SimulationResult baseline = NetworkSimulator.run(
                live, runtime.graph(), runtime.forecast(),
                NetworkSimulator.Policy.FLOWFORGE, 28, properties.getSimulation().getSeed()
        );
        NetworkProposal proposal = override != null && hasPayload(override)
                ? override
                : ("openai".equalsIgnoreCase(properties.getAgent().getProvider())
                ? llmPlanner.propose(live, baseline)
                : HeuristicPlanner.propose(live, baseline));
        ProposalVerifier.Decision decision = verifier.evaluate(
                live, runtime.graph(), runtime.forecast(), proposal, 28, properties.getSimulation().getSeed()
        );
        if (applyIfAccepted && "ACCEPT".equals(decision.status())) {
            runtime.replace(ProposalVerifier.apply(live, proposal));
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", decision.status());
        body.put("reason", decision.reason());
        body.put("proposal", proposal);
        body.put("beforeFillRate", decision.before() == null ? null : decision.before().fillRate());
        body.put("afterFillRate", decision.after() == null ? null : decision.after().fillRate());
        body.put("beforeCost", decision.before() == null ? null : decision.before().totalCost());
        body.put("afterCost", decision.after() == null ? null : decision.after().totalCost());
        body.put("applied", applyIfAccepted && "ACCEPT".equals(decision.status()));
        return body;
    }

    @GetMapping("/forecast/{regionId}")
    public Map<String, Object> forecast(@PathVariable String regionId) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("regionId", regionId);
        body.put("dailyMean", runtime.forecast().get(regionId));
        List<M5Series> related = runtime.catalog().stream()
                .filter(s -> s.storeId().equals(runtime.network().region(regionId).assignedNodeId()))
                .limit(3)
                .toList();
        List<Map<String, Object>> series = new ArrayList<>();
        for (M5Series item : related) {
            DemandForecaster.Forecast forecast = DemandForecaster.forecast(
                    item.sales(), properties.getForecast().getHorizonDays()
            );
            series.add(Map.of(
                    "id", item.id(),
                    "itemId", item.itemId(),
                    "method", forecast.method(),
                    "horizon", forecast.horizon(),
                    "dailyMean", forecast.dailyMean()
            ));
        }
        body.put("skuForecasts", series);
        return body;
    }

    @GetMapping("/inventory")
    public List<Map<String, Object>> inventory() {
        return runtime.network().fulfillmentNodes().stream()
                .map(n -> Map.<String, Object>of(
                        "id", n.id(),
                        "name", n.name(),
                        "onHand", n.onHand(),
                        "safetyStock", n.safetyStock(),
                        "capacity", n.storageCapacity(),
                        "utilization", n.storageCapacity() == 0 ? 0 : n.onHand() / (double) n.storageCapacity()
                ))
                .toList();
    }

    @PostMapping(value = "/bench/routing", produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> bench(@RequestParam(defaultValue = "400") int iterations) {
        var network = runtime.network();
        var graph = runtime.graph();
        List<String> regions = network.regions().stream().map(r -> r.id()).toList();
        List<Long> samples = new ArrayList<>(iterations);
        RouteDecision last = null;
        for (int i = 0; i < iterations; i++) {
            String regionId = regions.get(i % regions.size());
            long start = System.nanoTime();
            last = RoutingEngine.route(
                    network,
                    graph,
                    new RoutingEngine.Request(regionId, i % 24, 24, RoutingEngine.Algorithm.AUTO, false)
            );
            samples.add((System.nanoTime() - start) / 1_000_000);
        }
        samples.sort(Comparator.naturalOrder());
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("iterations", iterations);
        body.put("p50Ms", percentile(samples, 0.50));
        body.put("p95Ms", percentile(samples, 0.95));
        body.put("p99Ms", percentile(samples, 0.99));
        body.put("lastDecision", last);
        return body;
    }

    @GetMapping("/metrics/summary")
    public Map<String, Object> metrics() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("nodes", runtime.network().fulfillmentNodes().size());
        body.put("regions", runtime.network().regions().size());
        body.put("lanes", runtime.network().lanes().size());
        body.put("skus", runtime.catalog().size());
        body.put("forecastRegions", runtime.forecast().size());
        return body;
    }

    private static boolean hasPayload(NetworkProposal proposal) {
        return (proposal.safetyStockDeltas() != null && !proposal.safetyStockDeltas().isEmpty())
                || (proposal.laneCapacityDeltas() != null && !proposal.laneCapacityDeltas().isEmpty())
                || (proposal.assignments() != null && !proposal.assignments().isEmpty());
    }

    private static long percentile(List<Long> sorted, double p) {
        if (sorted.isEmpty()) {
            return 0;
        }
        int idx = Math.min(sorted.size() - 1, (int) Math.floor(p * (sorted.size() - 1)));
        return sorted.get(idx);
    }
}
