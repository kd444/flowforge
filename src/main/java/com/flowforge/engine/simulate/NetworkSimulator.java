package com.flowforge.engine.simulate;

import com.flowforge.domain.DemandRegion;
import com.flowforge.domain.FulfillmentNetwork;
import com.flowforge.domain.NetworkNode;
import com.flowforge.domain.TransportMode;
import com.flowforge.engine.graph.TimeDependentGraph;
import com.flowforge.engine.routing.RouteDecision;
import com.flowforge.engine.routing.RoutingEngine;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Deterministic period simulator used both for offline evaluation and as the
 * agentic control-loop verifier.
 */
public final class NetworkSimulator {

    public enum Policy {
        FLOWFORGE,
        GREEDY
    }

    private NetworkSimulator() {
    }

    public static SimulationResult run(
            FulfillmentNetwork seed,
            TimeDependentGraph graph,
            Map<String, Double> regionForecast,
            Policy policy,
            int days,
            long rngSeed
    ) {
        FulfillmentNetwork network = seed.copy();
        Random random = new Random(rngSeed);
        int n = days;
        Map<String, int[]> opening = new HashMap<>();
        Map<String, int[]> inbound = new HashMap<>();
        Map<String, int[]> outbound = new HashMap<>();
        Map<String, int[]> fulfilled = new HashMap<>();
        Map<String, int[]> closing = new HashMap<>();
        Map<String, Integer> stock = new HashMap<>();

        int initial = 0;
        for (NetworkNode node : network.fulfillmentNodes()) {
            stock.put(node.id(), node.onHand());
            initial += node.onHand();
            opening.put(node.id(), new int[n]);
            inbound.put(node.id(), new int[n]);
            outbound.put(node.id(), new int[n]);
            fulfilled.put(node.id(), new int[n]);
            closing.put(node.id(), new int[n]);
        }

        int totalFulfilled = 0;
        int totalUnmet = 0;
        int totalReceipts = 0;
        int orders = 0;
        double transport = 0;
        double holding = 0;
        double dailySystem = regionForecast.values().stream().mapToDouble(Double::doubleValue).sum();
        List<SimulationResult.DailySnapshot> timeline = new ArrayList<>();

        for (int day = 0; day < n; day++) {
            int dayFulfilled = 0;
            int dayUnmet = 0;
            double dayCost = 0;
            int hour = 8 + (day % 3);

            for (NetworkNode node : network.fulfillmentNodes()) {
                opening.get(node.id())[day] = stock.get(node.id());
                int room = Math.max(0, node.storageCapacity() - stock.get(node.id()));
                int target = Math.max(node.safetyStock(), (int) Math.round(dailySystem * 0.09));
                int receipt = Math.min(room, Math.max(0, target - stock.get(node.id())));
                if (receipt > 0) {
                    stock.put(node.id(), stock.get(node.id()) + receipt);
                    inbound.get(node.id())[day] += receipt;
                    totalReceipts += receipt;
                }
            }

            List<DemandRegion> regions = new ArrayList<>(network.regions());
            for (DemandRegion region : regions) {
                double mean = regionForecast.getOrDefault(region.id(), 6.0);
                int demand = poisson(random, mean);
                if (demand <= 0) {
                    continue;
                }
                orders += demand;
                int remaining = demand;
                if (policy == Policy.FLOWFORGE) {
                    RouteDecision decision = RoutingEngine.route(
                            withStock(network, stock),
                            graph,
                            new RoutingEngine.Request(region.id(), hour, 24, RoutingEngine.Algorithm.ASTAR, true)
                    );
                    if (decision.slaMet() && decision.sourceNodeId() != null) {
                        int have = stock.getOrDefault(decision.sourceNodeId(), 0);
                        int take = Math.min(have, remaining);
                        if (take > 0) {
                            stock.put(decision.sourceNodeId(), have - take);
                            fulfilled.get(decision.sourceNodeId())[day] += take;
                            remaining -= take;
                            dayFulfilled += take;
                            totalFulfilled += take;
                            double cost = decision.cost() * take;
                            transport += cost;
                            dayCost += cost;
                        }
                    }
                } else {
                    NetworkNode nearest = nearestWithStock(network, stock, region);
                    if (nearest != null) {
                        int have = stock.getOrDefault(nearest.id(), 0);
                        int take = Math.min(have, remaining);
                        stock.put(nearest.id(), have - take);
                        fulfilled.get(nearest.id())[day] += take;
                        remaining -= take;
                        dayFulfilled += take;
                        totalFulfilled += take;
                        double km = Math.max(4.0, nearest.location().haversineKm(region.location()));
                        double unit = km * TransportMode.LAST_MILE.costPerKm()
                                * TimeDependentGraph.congestionMultiplier(TransportMode.LAST_MILE, hour)
                                + 14.0;
                        double cost = take * unit;
                        transport += cost;
                        dayCost += cost;
                    }
                }
                dayUnmet += remaining;
                totalUnmet += remaining;
            }

            for (NetworkNode node : network.fulfillmentNodes()) {
                int onHand = stock.get(node.id());
                closing.get(node.id())[day] = onHand;
                double hold = onHand * node.holdingCostPerUnit();
                holding += hold;
                dayCost += hold;
            }
            int soFar = totalFulfilled + totalUnmet;
            timeline.add(new SimulationResult.DailySnapshot(
                    day,
                    dayFulfilled,
                    dayUnmet,
                    dayCost,
                    soFar == 0 ? 1.0 : totalFulfilled / (double) soFar
            ));
        }

        InvariantReport local = FlowConservationChecker.check(opening, inbound, outbound, fulfilled, closing);
        int ending = stock.values().stream().mapToInt(Integer::intValue).sum();
        InvariantReport global = FlowConservationChecker.checkGlobal(
                initial, totalReceipts, ending, 0, totalFulfilled
        );
        for (String v : global.violations()) {
            local.add(v);
        }

        Map<String, Integer> endingMap = new LinkedHashMap<>(stock);
        int denom = totalFulfilled + totalUnmet;
        return new SimulationResult(
                policy.name(),
                days,
                orders,
                totalFulfilled,
                totalUnmet,
                denom == 0 ? 1.0 : totalFulfilled / (double) denom,
                transport + holding,
                holding,
                transport,
                totalFulfilled == 0 ? 0 : (transport + holding) / totalFulfilled,
                local,
                endingMap,
                timeline
        );
    }

    private static FulfillmentNetwork withStock(FulfillmentNetwork network, Map<String, Integer> stock) {
        FulfillmentNetwork copy = network.copy();
        for (NetworkNode node : copy.fulfillmentNodes()) {
            copy.replaceNode(node.withOnHand(stock.getOrDefault(node.id(), 0)));
        }
        return copy;
    }

    private static NetworkNode nearestWithStock(
            FulfillmentNetwork network,
            Map<String, Integer> stock,
            DemandRegion region
    ) {
        NetworkNode best = null;
        double bestKm = Double.MAX_VALUE;
        for (NetworkNode node : network.fulfillmentNodes()) {
            if (stock.getOrDefault(node.id(), 0) <= 0) {
                continue;
            }
            double km = node.location().haversineKm(region.location());
            if (km < bestKm) {
                bestKm = km;
                best = node;
            }
        }
        return best;
    }

    private static int poisson(Random random, double lambda) {
        double l = Math.max(0.1, lambda);
        if (l < 30) {
            double p = 1.0;
            int k = 0;
            double bound = Math.exp(-l);
            do {
                k++;
                p *= random.nextDouble();
            } while (p > bound);
            return k - 1;
        }
        double g = random.nextGaussian();
        return Math.max(0, (int) Math.round(l + Math.sqrt(l) * g));
    }

    public static Comparison compare(SimulationResult flowforge, SimulationResult greedy) {
        double costDelta = greedy.totalCost() == 0 ? 0 : (greedy.totalCost() - flowforge.totalCost()) / greedy.totalCost();
        return new Comparison(flowforge, greedy, costDelta, flowforge.fillRate() - greedy.fillRate());
    }

    public record Comparison(
            SimulationResult flowforge,
            SimulationResult greedy,
            double costReduction,
            double fillRateDelta
    ) {
    }
}
