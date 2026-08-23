package com.flowforge.domain;

public record Lane(
        String id,
        String fromId,
        String toId,
        TransportMode mode,
        double distanceKm,
        int capacity,
        double toll
) {
    public Lane withCapacity(int next) {
        return new Lane(id, fromId, toId, mode, distanceKm, next, toll);
    }

    public static String idOf(String from, String to, TransportMode mode) {
        return from + "->" + to + ":" + mode.name();
    }
}
