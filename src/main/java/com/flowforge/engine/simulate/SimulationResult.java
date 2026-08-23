package com.flowforge.engine.simulate;

import java.util.List;
import java.util.Map;

public record SimulationResult(
        String policy,
        int days,
        int orders,
        int fulfilledUnits,
        int unmetUnits,
        double fillRate,
        double totalCost,
        double holdingCost,
        double transportCost,
        double costPerFulfilled,
        InvariantReport invariants,
        Map<String, Integer> endingInventory,
        List<DailySnapshot> timeline
) {
    public record DailySnapshot(int day, int fulfilled, int unmet, double cost, double fillRate) {
    }

    public boolean acceptedInvariants() {
        return invariants != null && invariants.ok();
    }
}
