package com.flowforge.engine.placement;

import com.flowforge.domain.DemandRegion;
import com.flowforge.domain.FulfillmentNetwork;
import com.flowforge.domain.NetworkNode;
import com.flowforge.engine.graph.TimeDependentGraph;
import com.flowforge.engine.routing.PathSearch;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Inventory placement as a transportation / min-cost flow problem
 * over the demand-forecast-weighted node-to-region graph.
 */
public final class InventoryPlacer {

    private InventoryPlacer() {
    }

    public static PlacementPlan place(
            FulfillmentNetwork network,
            TimeDependentGraph graph,
            Map<String, Double> regionForecast,
            int departHour,
            int slaHours
    ) {
        long start = System.nanoTime();
        List<NetworkNode> sources = network.fulfillmentNodes();
        List<DemandRegion> sinks = new ArrayList<>(network.regions());
        int s = sources.size();
        int t = sinks.size();
        int superSource = s + t;
        int superSink = s + t + 1;
        MinCostFlowSolver solver = new MinCostFlowSolver(s + t + 2);

        int totalSupply = 0;
        for (int i = 0; i < s; i++) {
            NetworkNode node = sources.get(i);
            int supply = Math.max(0, node.onHand());
            totalSupply += supply;
            solver.addEdge(superSource, i, supply, 0);
        }

        int totalDemand = 0;
        int[] demand = new int[t];
        for (int j = 0; j < t; j++) {
            DemandRegion region = sinks.get(j);
            int units = Math.max(1, (int) Math.round(regionForecast.getOrDefault(region.id(), 8.0)));
            demand[j] = units;
            totalDemand += units;
            solver.addEdge(s + j, superSink, units, 0);
        }

        long[][] unitCost = new long[s][t];
        for (int i = 0; i < s; i++) {
            NetworkNode node = sources.get(i);
            for (int j = 0; j < t; j++) {
                DemandRegion region = sinks.get(j);
                PathSearch.PathResult path = PathSearch.astar(
                        graph, network, node.id(), region.id(), departHour, slaHours
                );
                double route = path.found() ? path.cost() : 5_000.0;
                double hold = node.holdingCostPerUnit() * 4.0;
                long scaled = Math.max(1L, Math.round((route + hold) * 100.0));
                unitCost[i][j] = scaled;
                solver.addEdge(i, s + j, Math.min(node.storageCapacity(), demand[j] * 3), scaled);
            }
        }

        int ship = Math.min(totalSupply, totalDemand);
        MinCostFlowSolver.Result result = solver.minCostFlow(superSource, superSink, ship);

        Map<String, Integer> unitsAtNode = new LinkedHashMap<>();
        for (NetworkNode node : sources) {
            unitsAtNode.put(node.id(), 0);
        }
        List<PlacementPlan.Allocation> allocations = new ArrayList<>();
        for (int[] used : result.used()) {
            if (used[0] < s && used[1] >= s && used[1] < s + t) {
                NetworkNode node = sources.get(used[0]);
                DemandRegion region = sinks.get(used[1] - s);
                unitsAtNode.merge(node.id(), used[2], Integer::sum);
                allocations.add(new PlacementPlan.Allocation(
                        node.id(),
                        region.id(),
                        used[2],
                        unitCost[used[0]][used[1] - s] / 100.0
                ));
            }
        }
        long micros = (System.nanoTime() - start) / 1_000;
        return new PlacementPlan(
                unitsAtNode,
                allocations,
                result.cost() / 100.0,
                result.flow(),
                Math.max(0, totalDemand - result.flow()),
                micros
        );
    }
}
