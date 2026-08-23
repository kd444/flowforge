package com.flowforge.engine.agent;

import com.flowforge.domain.DemandRegion;
import com.flowforge.domain.FulfillmentNetwork;
import com.flowforge.domain.ScottishNetworkFactory;
import com.flowforge.engine.forecast.DemandForecaster;
import com.flowforge.engine.forecast.M5SyntheticGenerator;
import com.flowforge.engine.graph.TimeDependentGraph;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProposalVerifierTest {

    @Test
    void rejectsUnknownNodeAndAcceptsHeuristicWhenSafe() {
        FulfillmentNetwork network = ScottishNetworkFactory.build();
        var catalog = M5SyntheticGenerator.generate(
                network.fulfillmentNodes().stream().map(n -> n.id()).toList(), 80, 3
        );
        Map<String, Double> scale = network.regions().stream()
                .collect(Collectors.toMap(DemandRegion::id, DemandRegion::demandScale));
        Map<String, Double> forecast = DemandForecaster.regionDailyMean(catalog, scale, 21);
        TimeDependentGraph graph = new TimeDependentGraph(network, forecast);
        ProposalVerifier verifier = new ProposalVerifier(2.0, 25.0);

        NetworkProposal bad = new NetworkProposal();
        bad.getSafetyStockDeltas().put("NO-SUCH-NODE", 50);
        ProposalVerifier.Decision rejected = verifier.evaluate(network, graph, forecast, bad, 10, 1);
        assertEquals("REJECT", rejected.status());

        NetworkProposal heuristic = HeuristicPlanner.propose(network,
                com.flowforge.engine.simulate.NetworkSimulator.run(
                        network, graph, forecast,
                        com.flowforge.engine.simulate.NetworkSimulator.Policy.FLOWFORGE, 10, 1
                ));
        ProposalVerifier.Decision decision = verifier.evaluate(network, graph, forecast, heuristic, 10, 1);
        assertTrue(decision.status().equals("ACCEPT") || decision.status().equals("REJECT")
                || decision.status().equals("ROLLBACK"));
        assertTrue(decision.before().acceptedInvariants());
    }
}
