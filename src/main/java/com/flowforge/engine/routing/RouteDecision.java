package com.flowforge.engine.routing;

import java.util.List;

public record RouteDecision(
        String sourceNodeId,
        String regionId,
        List<RouteHop> hops,
        double cost,
        int etaHours,
        boolean slaMet,
        String algorithm,
        long computeMicros
) {
    public static RouteDecision infeasible(String regionId, String algorithm, long micros) {
        return new RouteDecision(null, regionId, List.of(), Double.POSITIVE_INFINITY, -1, false, algorithm, micros);
    }
}
