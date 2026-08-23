package com.flowforge.engine.agent;

import com.flowforge.domain.FulfillmentNetwork;
import com.flowforge.domain.Lane;
import com.flowforge.domain.NetworkNode;
import com.flowforge.engine.simulate.SimulationResult;

import java.util.Comparator;

/**
 * Offline planner used when no LLM key is configured.
 */
public final class HeuristicPlanner {

    private HeuristicPlanner() {
    }

    public static NetworkProposal propose(FulfillmentNetwork network, SimulationResult baseline) {
        NetworkProposal proposal = new NetworkProposal();
        proposal.setRationale(
                "Shift safety stock toward high-holding-efficiency central nodes and "
                        + "open spare trunk capacity on the Glasgow-Edinburgh spine."
        );

        NetworkNode glasgow = network.node("GLW-NDC");
        NetworkNode inverness = network.node("INV-RDC");
        proposal.safetyStockDeltas().put(glasgow.id(), 180);
        proposal.safetyStockDeltas().put(inverness.id(), -40);
        if (baseline.fillRate() < 0.97) {
            proposal.safetyStockDeltas().put("ABD-RDC", 80);
            proposal.safetyStockDeltas().put("EDI-RDC", 60);
        }

        network.lanes().stream()
                .filter(lane -> lane.fromId().equals("GLW-NDC") && lane.toId().equals("EDI-RDC"))
                .max(Comparator.comparingInt(Lane::capacity))
                .ifPresent(lane -> proposal.laneCapacityDeltas().put(lane.id(), 120));

        proposal.assignments().add(new NetworkProposal.Assignment("ABD-RDC", "R40", 1));
        proposal.assignments().add(new NetworkProposal.Assignment("INV-RDC", "R39", 1));
        proposal.assignments().add(new NetworkProposal.Assignment("INV-RDC", "R41", 1));
        return proposal;
    }
}
