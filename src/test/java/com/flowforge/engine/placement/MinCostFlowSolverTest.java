package com.flowforge.engine.placement;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MinCostFlowSolverTest {

    @Test
    void solvesClassicTransportationInstance() {
        MinCostFlowSolver solver = new MinCostFlowSolver(4);
        solver.addEdge(0, 2, 10, 2);
        solver.addEdge(0, 3, 10, 4);
        solver.addEdge(1, 2, 10, 5);
        solver.addEdge(1, 3, 10, 1);
        MinCostFlowSolver.Result result = solver.minCostFlow(0, 3, 0);
        assertEquals(0, result.flow());

        MinCostFlowSolver balanced = new MinCostFlowSolver(6);
        int s = 4;
        int t = 5;
        balanced.addEdge(s, 0, 20, 0);
        balanced.addEdge(s, 1, 10, 0);
        balanced.addEdge(0, 2, 20, 200);
        balanced.addEdge(0, 3, 20, 400);
        balanced.addEdge(1, 2, 20, 500);
        balanced.addEdge(1, 3, 20, 100);
        balanced.addEdge(2, t, 15, 0);
        balanced.addEdge(3, t, 15, 0);
        MinCostFlowSolver.Result shipped = balanced.minCostFlow(s, t, 30);
        assertEquals(30, shipped.flow());
        assertEquals(6000L, shipped.cost());
        assertTrue(shipped.used().size() >= 2);
    }
}
