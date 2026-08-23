package com.flowforge.engine.simulate;

import com.flowforge.domain.DemandRegion;
import com.flowforge.domain.FulfillmentNetwork;
import com.flowforge.domain.ScottishNetworkFactory;
import com.flowforge.engine.forecast.DemandForecaster;
import com.flowforge.engine.forecast.M5SyntheticGenerator;
import com.flowforge.engine.graph.TimeDependentGraph;
import com.flowforge.engine.placement.InventoryPlacer;
import com.flowforge.engine.placement.PlacementPlan;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertTrue;

class NetworkSimulatorTest {

    @Test
    void flowforgeBeatsGreedyAndConservesFlow() {
        FulfillmentNetwork network = ScottishNetworkFactory.build();
        var catalog = M5SyntheticGenerator.generate(
                network.fulfillmentNodes().stream().map(n -> n.id()).toList(), 120, 7
        );
        Map<String, Double> scale = network.regions().stream()
                .collect(Collectors.toMap(DemandRegion::id, DemandRegion::demandScale));
        Map<String, Double> forecast = DemandForecaster.regionDailyMean(catalog, scale, 28);
        TimeDependentGraph graph = new TimeDependentGraph(network, forecast);

        SimulationResult flowforge = NetworkSimulator.run(
                network, graph, forecast, NetworkSimulator.Policy.FLOWFORGE, 21, 42
        );
        SimulationResult greedy = NetworkSimulator.run(
                network, graph, forecast, NetworkSimulator.Policy.GREEDY, 21, 42
        );

        assertTrue(flowforge.acceptedInvariants(), () -> flowforge.invariants().violations().toString());
        assertTrue(greedy.acceptedInvariants(), () -> greedy.invariants().violations().toString());
        assertTrue(flowforge.fillRate() >= 0.90, "fill rate " + flowforge.fillRate());
        assertTrue(flowforge.totalCost() < greedy.totalCost(),
                "expected FlowForge cheaper: " + flowforge.totalCost() + " vs " + greedy.totalCost());
    }

    @Test
    void placementRespectsSupply() {
        FulfillmentNetwork network = ScottishNetworkFactory.build();
        Map<String, Double> forecast = network.regions().stream()
                .collect(Collectors.toMap(DemandRegion::id, r -> 8.0));
        TimeDependentGraph graph = new TimeDependentGraph(network, forecast);
        PlacementPlan plan = InventoryPlacer.place(network, graph, forecast, 9, 24);
        int placed = plan.unitsAtNode().values().stream().mapToInt(Integer::intValue).sum();
        int supply = network.fulfillmentNodes().stream().mapToInt(n -> n.onHand()).sum();
        assertTrue(plan.totalFlow() > 0);
        assertTrue(placed <= supply);
        assertTrue(plan.unmetDemand() >= 0);
    }
}
