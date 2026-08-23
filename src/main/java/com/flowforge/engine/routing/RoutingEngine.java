package com.flowforge.engine.routing;

import com.flowforge.domain.FulfillmentNetwork;
import com.flowforge.domain.NetworkNode;
import com.flowforge.engine.graph.TimeDependentGraph;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Chooses a source node and multi-modal lane set that meets the delivery-promise SLA.
 */
public final class RoutingEngine {

    public enum Algorithm {
        ASTAR,
        BIDIRECTIONAL_DIJKSTRA,
        AUTO
    }

    public record Request(
            String regionId,
            int departHour,
            int slaHours,
            Algorithm algorithm,
            boolean requireStock
    ) {
        public Request {
            if (algorithm == null) {
                algorithm = Algorithm.AUTO;
            }
            if (slaHours <= 0) {
                slaHours = 24;
            }
            departHour = Math.floorMod(departHour, 24);
        }
    }

    private RoutingEngine() {
    }

    public static RouteDecision route(
            FulfillmentNetwork network,
            TimeDependentGraph graph,
            Request request
    ) {
        long start = System.nanoTime();
        List<NetworkNode> candidates = new ArrayList<>(network.fulfillmentNodes());
        if (request.requireStock()) {
            candidates.removeIf(node -> node.onHand() <= 0);
        }
        candidates.sort(Comparator.comparingDouble((NetworkNode node) ->
                node.location().haversineKm(network.region(request.regionId()).location())));

        Algorithm chosen = request.algorithm() == Algorithm.AUTO
                ? (candidates.size() > 6 ? Algorithm.ASTAR : Algorithm.BIDIRECTIONAL_DIJKSTRA)
                : request.algorithm();

        RouteDecision best = RouteDecision.infeasible(request.regionId(), chosen.name(), 0);
        for (NetworkNode source : candidates) {
            PathSearch.PathResult path = search(
                    graph, network, source.id(), request.regionId(),
                    request.departHour(), request.slaHours(), chosen
            );
            if (!path.found()) {
                continue;
            }
            if (path.cost() < best.cost()) {
                best = new RouteDecision(
                        source.id(),
                        request.regionId(),
                        path.hops(),
                        path.cost(),
                        path.etaHours(),
                        path.etaHours() <= request.slaHours(),
                        chosen.name(),
                        0
                );
            }
        }
        long micros = (System.nanoTime() - start) / 1_000;
        return new RouteDecision(
                best.sourceNodeId(),
                best.regionId(),
                best.hops(),
                best.cost(),
                best.etaHours(),
                best.slaMet(),
                best.algorithm(),
                micros
        );
    }

    public static PathSearch.PathResult search(
            TimeDependentGraph graph,
            FulfillmentNetwork network,
            String source,
            String target,
            int departHour,
            int slaHours,
            Algorithm algorithm
    ) {
        if (algorithm == Algorithm.BIDIRECTIONAL_DIJKSTRA) {
            return PathSearch.bidirectionalDijkstra(graph, network, source, target, departHour, slaHours);
        }
        return PathSearch.astar(graph, network, source, target, departHour, slaHours);
    }

    public static Map<String, Double> unitRouteCost(
            FulfillmentNetwork network,
            TimeDependentGraph graph,
            int departHour,
            int slaHours
    ) {
        Map<String, Double> costs = new java.util.LinkedHashMap<>();
        for (NetworkNode node : network.fulfillmentNodes()) {
            for (var region : network.regions()) {
                PathSearch.PathResult path = PathSearch.astar(
                        graph, network, node.id(), region.id(), departHour, slaHours
                );
                costs.put(node.id() + "->" + region.id(), path.found() ? path.cost() : 1_000_000.0);
            }
        }
        return costs;
    }
}
