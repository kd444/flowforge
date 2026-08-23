package com.flowforge.domain;

public record DemandRegion(
        String id,
        String name,
        GeoPoint location,
        String assignedNodeId,
        double demandScale
) {
    public DemandRegion withAssignment(String nodeId) {
        return new DemandRegion(id, name, location, nodeId, demandScale);
    }
}
