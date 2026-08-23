package com.flowforge.engine.placement;

import java.util.List;
import java.util.Map;

public record PlacementPlan(
        Map<String, Integer> unitsAtNode,
        List<Allocation> allocations,
        double totalCost,
        int totalFlow,
        int unmetDemand,
        long computeMicros
) {
    public record Allocation(String nodeId, String regionId, int units, double unitCost) {
    }
}
