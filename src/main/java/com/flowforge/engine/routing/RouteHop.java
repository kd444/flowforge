package com.flowforge.engine.routing;

import com.flowforge.domain.TransportMode;

public record RouteHop(
        String fromId,
        String toId,
        TransportMode mode,
        String laneId,
        double cost,
        int departHour,
        int arriveHour
) {
}
