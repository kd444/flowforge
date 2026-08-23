package com.flowforge.engine.agent;

import com.flowforge.domain.DemandRegion;
import com.flowforge.domain.FulfillmentNetwork;
import com.flowforge.domain.Lane;
import com.flowforge.domain.NetworkNode;
import com.flowforge.engine.graph.TimeDependentGraph;
import com.flowforge.engine.simulate.NetworkSimulator;
import com.flowforge.engine.simulate.SimulationResult;

import java.util.Map;

public final class ProposalVerifier {

    public record Decision(
            String status,
            String reason,
            NetworkProposal proposal,
            SimulationResult before,
            SimulationResult after
    ) {
    }

    private final double maxFillDropPp;
    private final double maxCostIncreasePct;

    public ProposalVerifier(double maxFillDropPp, double maxCostIncreasePct) {
        this.maxFillDropPp = maxFillDropPp;
        this.maxCostIncreasePct = maxCostIncreasePct;
    }

    public Decision evaluate(
            FulfillmentNetwork live,
            TimeDependentGraph graph,
            Map<String, Double> forecast,
            NetworkProposal proposal,
            int days,
            long seed
    ) {
        SimulationResult before = NetworkSimulator.run(
                live, graph, forecast, NetworkSimulator.Policy.FLOWFORGE, days, seed
        );
        if (!before.acceptedInvariants()) {
            return new Decision("REJECT", "Baseline simulator failed conservation.", proposal, before, before);
        }
        FulfillmentNetwork candidate;
        try {
            candidate = apply(live, proposal);
        } catch (RuntimeException ex) {
            return new Decision("REJECT", "Proposal could not be applied: " + ex.getMessage(), proposal, before, null);
        }
        TimeDependentGraph nextGraph = new TimeDependentGraph(candidate, forecast);
        SimulationResult after = NetworkSimulator.run(
                candidate, nextGraph, forecast, NetworkSimulator.Policy.FLOWFORGE, days, seed
        );
        if (!after.acceptedInvariants()) {
            return new Decision("ROLLBACK", "Candidate broke flow conservation.", proposal, before, after);
        }
        double fillDrop = (before.fillRate() - after.fillRate()) * 100.0;
        double costUp = before.totalCost() == 0 ? 0 : (after.totalCost() - before.totalCost()) / before.totalCost() * 100.0;
        if (fillDrop > maxFillDropPp) {
            return new Decision("REJECT", "Fill rate dropped " + round(fillDrop) + "pp.", proposal, before, after);
        }
        if (costUp > maxCostIncreasePct && after.fillRate() <= before.fillRate() + 0.002) {
            return new Decision("REJECT", "Cost rose " + round(costUp) + "% without a fill-rate gain.", proposal, before, after);
        }
        return new Decision("ACCEPT", "Simulator improved or held service at acceptable cost.", proposal, before, after);
    }

    public static FulfillmentNetwork apply(FulfillmentNetwork live, NetworkProposal proposal) {
        FulfillmentNetwork copy = live.copy();
        proposal.safetyStockDeltas().forEach((nodeId, delta) -> {
            NetworkNode node = copy.node(nodeId);
            int nextSafety = Math.max(0, node.safetyStock() + delta);
            int nextOnHand = Math.min(node.storageCapacity(), Math.max(0, node.onHand() + delta));
            copy.replaceNode(node.withSafetyStock(nextSafety).withOnHand(nextOnHand));
        });
        proposal.laneCapacityDeltas().forEach((laneId, delta) -> {
            Lane lane = copy.lane(laneId);
            copy.replaceLane(lane.withCapacity(Math.max(0, lane.capacity() + delta)));
        });
        for (NetworkProposal.Assignment assignment : proposal.assignments()) {
            DemandRegion region = copy.region(assignment.regionId());
            copy.node(assignment.nodeId());
            copy.replaceRegion(region.withAssignment(assignment.nodeId()));
        }
        return copy;
    }

    private static String round(double value) {
        return String.format("%.2f", value);
    }
}
