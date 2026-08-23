package com.flowforge.engine.routing;

import com.flowforge.domain.FulfillmentNetwork;
import com.flowforge.domain.GeoPoint;
import com.flowforge.engine.graph.TimedEdge;
import com.flowforge.engine.graph.TimeDependentGraph;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.PriorityQueue;

/**
 * Constrained shortest-path search over the time-expanded graph.
 */
public final class PathSearch {

    public record PathResult(List<RouteHop> hops, double cost, int etaHours) {
        public static PathResult none() {
            return new PathResult(List.of(), Double.POSITIVE_INFINITY, -1);
        }

        public boolean found() {
            return etaHours >= 0;
        }
    }

    private record State(String node, int hour, int elapsed) {
    }

    private record Frontier(State state, double g, double f) {
    }

    private PathSearch() {
    }

    public static PathResult astar(
            TimeDependentGraph graph,
            FulfillmentNetwork network,
            String source,
            String target,
            int startHour,
            int slaHours
    ) {
        GeoPoint goal = network.locationOf(target);
        double minCostPerKm = 0.28;
        PriorityQueue<Frontier> open = new PriorityQueue<>(Comparator.comparingDouble(Frontier::f));
        Map<String, Double> bestG = new HashMap<>();
        Map<String, TimedEdge> cameFrom = new HashMap<>();
        Map<String, String> camePrev = new HashMap<>();
        Map<String, Integer> arriveHour = new HashMap<>();
        State start = new State(source, TimeDependentGraph.wrapHour(startHour), 0);
        open.add(new Frontier(start, 0.0, heuristic(network, source, goal, minCostPerKm)));
        bestG.put(source, 0.0);
        arriveHour.put(source, start.hour());

        String end = null;
        while (!open.isEmpty()) {
            Frontier cur = open.poll();
            if (cur.g > bestG.getOrDefault(cur.state.node(), Double.POSITIVE_INFINITY) + 1e-9) {
                continue;
            }
            if (cur.state.node().equals(target) && cur.state.elapsed() <= slaHours) {
                end = cur.state.node();
                break;
            }
            if (cur.state.elapsed() > slaHours) {
                continue;
            }
            for (TimedEdge edge : graph.outgoing(cur.state.node())) {
                if (edge.toId().equals(cur.state.node())) {
                    continue;
                }
                int travel = graph.travelHours(edge, cur.state.hour());
                int nextElapsed = cur.state.elapsed() + travel;
                if (nextElapsed > slaHours) {
                    continue;
                }
                double step = graph.traversalCost(edge, cur.state.hour());
                double ng = cur.g + step;
                if (ng + 1e-9 < bestG.getOrDefault(edge.toId(), Double.POSITIVE_INFINITY)) {
                    bestG.put(edge.toId(), ng);
                    cameFrom.put(edge.toId(), edge);
                    camePrev.put(edge.toId(), cur.state.node());
                    int nextHour = TimeDependentGraph.wrapHour(cur.state.hour() + travel);
                    arriveHour.put(edge.toId(), nextHour);
                    State nxt = new State(edge.toId(), nextHour, nextElapsed);
                    double h = heuristic(network, edge.toId(), goal, minCostPerKm);
                    open.add(new Frontier(nxt, ng, ng + h));
                }
            }
        }
        if (end == null) {
            return PathResult.none();
        }
        return reconstruct(graph, cameFrom, camePrev, source, end, startHour);
    }

    /**
     * Bidirectional Dijkstra: forward search uses live time-dependent costs,
     * backward search uses admissible night-time lower bounds, then the meeting
     * path is re-costed on the forward timeline.
     */
    public static PathResult bidirectionalDijkstra(
            TimeDependentGraph graph,
            FulfillmentNetwork network,
            String source,
            String target,
            int startHour,
            int slaHours
    ) {
        if (source.equals(target)) {
            return new PathResult(List.of(), 0.0, 0);
        }
        PriorityQueue<Frontier> fwd = new PriorityQueue<>(Comparator.comparingDouble(Frontier::g));
        PriorityQueue<Frontier> bwd = new PriorityQueue<>(Comparator.comparingDouble(Frontier::g));
        Map<State, Double> fg = new HashMap<>();
        Map<String, Double> bg = new HashMap<>();
        Map<State, TimedEdge> fEdge = new HashMap<>();
        Map<State, State> fPrev = new HashMap<>();
        Map<String, TimedEdge> bEdge = new HashMap<>();
        Map<String, String> bPrev = new HashMap<>();

        State start = new State(source, TimeDependentGraph.wrapHour(startHour), 0);
        fwd.add(new Frontier(start, 0.0, 0.0));
        fg.put(start, 0.0);
        bwd.add(new Frontier(new State(target, 0, 0), 0.0, 0.0));
        bg.put(target, 0.0);

        double best = Double.POSITIVE_INFINITY;
        State meetFwd = null;
        String meetNode = null;

        int expansions = 0;
        while (!fwd.isEmpty() && expansions < 8_000) {
            expansions++;
            Frontier cur = fwd.poll();
            if (cur.g > fg.getOrDefault(cur.state, Double.POSITIVE_INFINITY) + 1e-9) {
                continue;
            }
            if (cur.state.elapsed() > slaHours) {
                continue;
            }
            Double back = bg.get(cur.state.node());
            if (back != null && cur.g + back < best) {
                best = cur.g + back;
                meetFwd = cur.state;
                meetNode = cur.state.node();
            }
            for (TimedEdge edge : graph.outgoing(cur.state.node())) {
                int travel = graph.travelHours(edge, cur.state.hour());
                int nextElapsed = cur.state.elapsed() + travel;
                if (nextElapsed > slaHours) {
                    continue;
                }
                int nextHour = TimeDependentGraph.wrapHour(cur.state.hour() + travel);
                double ng = cur.g + graph.traversalCost(edge, cur.state.hour());
                State nxt = new State(edge.toId(), nextHour, nextElapsed);
                if (ng + 1e-9 < fg.getOrDefault(nxt, Double.POSITIVE_INFINITY)) {
                    fg.put(nxt, ng);
                    fEdge.put(nxt, edge);
                    fPrev.put(nxt, cur.state);
                    fwd.add(new Frontier(nxt, ng, ng));
                }
            }
            if (!bwd.isEmpty()) {
                Frontier bcur = bwd.poll();
                if (bcur.g <= bg.getOrDefault(bcur.state.node(), Double.POSITIVE_INFINITY) + 1e-9) {
                    for (TimedEdge edge : graph.incoming(bcur.state.node())) {
                        double ng = bcur.g + graph.lowerBoundCost(edge);
                        if (ng + 1e-9 < bg.getOrDefault(edge.fromId(), Double.POSITIVE_INFINITY)) {
                            bg.put(edge.fromId(), ng);
                            bEdge.put(edge.fromId(), edge);
                            bPrev.put(edge.fromId(), bcur.state.node());
                            bwd.add(new Frontier(new State(edge.fromId(), 0, 0), ng, ng));
                        }
                    }
                }
            }
        }

        if (meetFwd == null || meetNode == null) {
            return astar(graph, network, source, target, startHour, slaHours);
        }
        List<TimedEdge> prefix = new ArrayList<>();
        State cursor = meetFwd;
        while (!cursor.equals(start)) {
            TimedEdge edge = fEdge.get(cursor);
            if (edge == null) {
                return PathResult.none();
            }
            prefix.add(edge);
            cursor = fPrev.get(cursor);
        }
        Collections.reverse(prefix);
        List<TimedEdge> suffix = new ArrayList<>();
        String node = meetNode;
        while (!node.equals(target)) {
            TimedEdge edge = bEdge.get(node);
            if (edge == null) {
                return astar(graph, network, source, target, startHour, slaHours);
            }
            suffix.add(edge);
            node = bPrev.get(node);
        }
        List<TimedEdge> all = new ArrayList<>(prefix);
        all.addAll(suffix);
        return materialize(graph, all, startHour, slaHours);
    }

    private static PathResult reconstruct(
            TimeDependentGraph graph,
            Map<String, TimedEdge> cameFrom,
            Map<String, String> camePrev,
            String start,
            String end,
            int startHour
    ) {
        List<TimedEdge> edges = new ArrayList<>();
        String cursor = end;
        int guard = 0;
        while (!cursor.equals(start)) {
            TimedEdge edge = cameFrom.get(cursor);
            if (edge == null || guard++ > 64) {
                return PathResult.none();
            }
            edges.add(edge);
            cursor = camePrev.get(cursor);
            if (cursor == null) {
                return PathResult.none();
            }
        }
        Collections.reverse(edges);
        return materialize(graph, edges, startHour, 10_000);
    }

    private static PathResult materialize(TimeDependentGraph graph, List<TimedEdge> edges, int startHour, int slaHours) {
        List<RouteHop> hops = new ArrayList<>();
        double cost = 0.0;
        int hour = startHour;
        int eta = 0;
        for (TimedEdge edge : edges) {
            double step = graph.traversalCost(edge, hour);
            int travel = graph.travelHours(edge, hour);
            int arrive = hour + travel;
            hops.add(new RouteHop(
                    edge.fromId(), edge.toId(), edge.mode(), edge.laneId(),
                    step, TimeDependentGraph.wrapHour(hour), TimeDependentGraph.wrapHour(arrive)
            ));
            cost += step;
            hour = arrive;
            eta += travel;
        }
        if (eta > slaHours) {
            return PathResult.none();
        }
        return new PathResult(hops, cost, eta);
    }

    private static double heuristic(FulfillmentNetwork network, String fromId, GeoPoint goal, double minCostPerKm) {
        try {
            return network.locationOf(fromId).haversineKm(goal) * minCostPerKm;
        } catch (RuntimeException ex) {
            return 0.0;
        }
    }

    public static boolean samePath(PathResult a, PathResult b) {
        return a.found() && b.found() && Objects.equals(terminals(a), terminals(b));
    }

    private static List<String> terminals(PathResult result) {
        if (result.hops().isEmpty()) {
            return List.of();
        }
        List<String> nodes = new ArrayList<>();
        nodes.add(result.hops().get(0).fromId());
        for (RouteHop hop : result.hops()) {
            nodes.add(hop.toId());
        }
        return nodes;
    }
}
