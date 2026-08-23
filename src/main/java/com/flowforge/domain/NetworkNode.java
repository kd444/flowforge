package com.flowforge.domain;

public record NetworkNode(
        String id,
        String name,
        NodeType type,
        GeoPoint location,
        int storageCapacity,
        double holdingCostPerUnit,
        int safetyStock,
        int onHand
) {
    public NetworkNode withSafetyStock(int next) {
        return new NetworkNode(id, name, type, location, storageCapacity, holdingCostPerUnit, next, onHand);
    }

    public NetworkNode withOnHand(int next) {
        return new NetworkNode(id, name, type, location, storageCapacity, holdingCostPerUnit, safetyStock, next);
    }

    public boolean isFulfillment() {
        return type == NodeType.NATIONAL_DC
                || type == NodeType.REGIONAL_DC
                || type == NodeType.FULFILLMENT_CENTER;
    }
}
