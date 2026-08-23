package com.flowforge.engine.routing;

import com.flowforge.domain.FulfillmentNetwork;
import com.flowforge.domain.ScottishNetworkFactory;
import com.flowforge.engine.graph.TimeDependentGraph;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RoutingEngineTest {

    private FulfillmentNetwork network;
    private TimeDependentGraph graph;

    @BeforeEach
    void setUp() {
        network = ScottishNetworkFactory.build();
        graph = new TimeDependentGraph(network, Map.of("R01", 1.8, "R40", 0.2));
    }

    @Test
    void astarFindsSlaFeasibleLocalRoute() {
        PathSearch.PathResult path = PathSearch.astar(graph, network, "GLW-NDC", "R01", 9, 24);
        assertTrue(path.found());
        assertTrue(path.etaHours() <= 24);
        assertTrue(path.cost() > 0);
        assertEquals("R01", path.hops().get(path.hops().size() - 1).toId());
    }

    @Test
    void bidirectionalAgreesOnLocalTerminal() {
        PathSearch.PathResult a = PathSearch.astar(graph, network, "EDI-RDC", "R04", 10, 24);
        PathSearch.PathResult b = PathSearch.bidirectionalDijkstra(graph, network, "EDI-RDC", "R04", 10, 24);
        assertTrue(a.found());
        assertTrue(b.found());
        assertEquals("R04", a.hops().get(a.hops().size() - 1).toId());
        assertEquals("R04", b.hops().get(b.hops().size() - 1).toId());
    }

    @Test
    void tightSlaCanRejectDistantIsland() {
        PathSearch.PathResult path = PathSearch.astar(graph, network, "KIL-FC", "R40", 8, 1);
        assertFalse(path.found());
    }

    @Test
    void engineSelectsStockedSource() {
        RouteDecision decision = RoutingEngine.route(
                network,
                graph,
                new RoutingEngine.Request("R07", 11, 24, RoutingEngine.Algorithm.ASTAR, true)
        );
        assertTrue(decision.slaMet());
        assertTrue(decision.sourceNodeId() != null);
        assertTrue(network.node(decision.sourceNodeId()).onHand() > 0);
        assertTrue(decision.computeMicros() >= 0);
    }
}
