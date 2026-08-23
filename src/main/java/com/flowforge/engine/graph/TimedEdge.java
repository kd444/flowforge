package com.flowforge.engine.graph;

import com.flowforge.domain.TransportMode;

public record TimedEdge(
        String laneId,
        String fromId,
        String toId,
        TransportMode mode,
        double distanceKm,
        int capacity,
        double toll
) {
}
