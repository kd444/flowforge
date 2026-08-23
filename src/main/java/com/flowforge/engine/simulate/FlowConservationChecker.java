package com.flowforge.engine.simulate;

import java.util.Map;

/**
 * Flow-conservation invariant: for every node and period,
 * opening + inbound - outbound - fulfilled == closing.
 */
public final class FlowConservationChecker {

    private static final double EPS = 1e-6;

    private FlowConservationChecker() {
    }

    public static InvariantReport check(
            Map<String, int[]> opening,
            Map<String, int[]> inbound,
            Map<String, int[]> outbound,
            Map<String, int[]> fulfilled,
            Map<String, int[]> closing
    ) {
        InvariantReport report = new InvariantReport();
        for (String node : opening.keySet()) {
            int[] o = opening.get(node);
            int[] in = inbound.getOrDefault(node, zeros(o.length));
            int[] out = outbound.getOrDefault(node, zeros(o.length));
            int[] fill = fulfilled.getOrDefault(node, zeros(o.length));
            int[] c = closing.get(node);
            if (c == null || c.length != o.length) {
                report.add("Missing closing inventory for " + node);
                continue;
            }
            for (int t = 0; t < o.length; t++) {
                double expected = o[t] + in[t] - out[t] - fill[t];
                if (Math.abs(expected - c[t]) > EPS) {
                    report.add("Conservation broken at " + node + " t=" + t
                            + " expected=" + expected + " closing=" + c[t]);
                    if (report.violations().size() >= 12) {
                        return report;
                    }
                }
            }
        }
        return report;
    }

    public static InvariantReport checkGlobal(
            int initialUnits,
            int receipts,
            int endingInventory,
            int inTransit,
            int fulfilled
    ) {
        InvariantReport report = new InvariantReport();
        int rhs = endingInventory + inTransit + fulfilled;
        int lhs = initialUnits + receipts;
        if (lhs != rhs) {
            report.add("Global conservation failed: initial+receipts=" + lhs
                    + " ending+inTransit+fulfilled=" + rhs);
        }
        return report;
    }

    private static int[] zeros(int n) {
        return new int[n];
    }
}
