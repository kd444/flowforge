package com.flowforge.domain;

/**
 * Multi-modal lanes used by the fulfillment graph.
 */
public enum TransportMode {
    TRUNK_ROAD(70.0, 0.42, 1.00),
    RAIL(90.0, 0.28, 1.15),
    LAST_MILE(38.0, 0.95, 0.70),
    FERRY(22.0, 1.35, 2.40),
    AIR(720.0, 4.80, 0.35);

    private final double cruiseKmh;
    private final double costPerKm;
    private final double slaSlack;

    TransportMode(double cruiseKmh, double costPerKm, double slaSlack) {
        this.cruiseKmh = cruiseKmh;
        this.costPerKm = costPerKm;
        this.slaSlack = slaSlack;
    }

    public double cruiseKmh() {
        return cruiseKmh;
    }

    public double costPerKm() {
        return costPerKm;
    }

    public double slaSlack() {
        return slaSlack;
    }
}
