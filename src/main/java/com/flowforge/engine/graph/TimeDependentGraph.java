package com.flowforge.engine.graph;

import com.flowforge.domain.FulfillmentNetwork;
import com.flowforge.domain.Lane;
import com.flowforge.domain.TransportMode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Demand-forecast-weighted, time-dependent residual graph.
 * Peak-hour congestion is FIFO-compliant so shortest-path labels stay consistent.
 */
public final class TimeDependentGraph {

    private final Map<String, List<TimedEdge>> outgoing = new HashMap<>();
    private final Map<String, List<TimedEdge>> incoming = new HashMap<>();
    private final Map<String, Double> demandWeight;

    public TimeDependentGraph(FulfillmentNetwork network, Map<String, Double> demandWeight) {
        this.demandWeight = normalize(demandWeight);
        for (Lane lane : network.lanes()) {
            TimedEdge edge = new TimedEdge(
                    lane.id(), lane.fromId(), lane.toId(), lane.mode(),
                    lane.distanceKm(), lane.capacity(), lane.toll()
            );
            outgoing.computeIfAbsent(lane.fromId(), key -> new ArrayList<>()).add(edge);
            incoming.computeIfAbsent(lane.toId(), key -> new ArrayList<>()).add(edge);
        }
    }

    public List<TimedEdge> outgoing(String nodeId) {
        return outgoing.getOrDefault(nodeId, List.of());
    }

    public List<TimedEdge> incoming(String nodeId) {
        return incoming.getOrDefault(nodeId, List.of());
    }

    public double traversalCost(TimedEdge edge, int hour) {
        double congestion = congestionMultiplier(edge.mode(), hour);
        double weight = 1.0 + 0.15 * demandWeight.getOrDefault(edge.toId(), 0.0);
        return edge.toll() + edge.distanceKm() * edge.mode().costPerKm() * congestion * weight;
    }

    public double lowerBoundCost(TimedEdge edge) {
        double night = congestionMultiplier(edge.mode(), 2);
        return edge.toll() + edge.distanceKm() * edge.mode().costPerKm() * night;
    }

    public int travelHours(TimedEdge edge, int hour) {
        double speed = edge.mode().cruiseKmh() / congestionMultiplier(edge.mode(), hour);
        int hours = (int) Math.ceil(edge.distanceKm() / Math.max(8.0, speed));
        return Math.max(1, Math.min(18, hours));
    }

    public static double congestionMultiplier(TransportMode mode, int hour) {
        int h = Math.floorMod(hour, 24);
        if (mode == TransportMode.AIR || mode == TransportMode.FERRY) {
            return (h >= 22 || h < 6) ? 0.92 : 1.05;
        }
        if (h >= 7 && h <= 9) {
            return 1.45;
        }
        if (h >= 16 && h <= 18) {
            return 1.38;
        }
        if (h >= 11 && h <= 14) {
            return 1.12;
        }
        if (h >= 22 || h < 5) {
            return 0.82;
        }
        return 1.00;
    }

    public static int wrapHour(int hour) {
        return Math.floorMod(hour, 24);
    }

    private static Map<String, Double> normalize(Map<String, Double> raw) {
        if (raw == null || raw.isEmpty()) {
            return Map.of();
        }
        double max = raw.values().stream().mapToDouble(Double::doubleValue).max().orElse(1.0);
        if (max <= 0) {
            return Map.of();
        }
        Map<String, Double> scaled = new HashMap<>();
        raw.forEach((key, value) -> scaled.put(key, value / max));
        return scaled;
    }
}
