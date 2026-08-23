package com.flowforge.domain;

public record GeoPoint(double lat, double lon) {

    private static final double EARTH_KM = 6371.0;

    public double haversineKm(GeoPoint other) {
        double dLat = Math.toRadians(other.lat - lat);
        double dLon = Math.toRadians(other.lon - lon);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat)) * Math.cos(Math.toRadians(other.lat))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return 2 * EARTH_KM * Math.asin(Math.min(1.0, Math.sqrt(a)));
    }
}
