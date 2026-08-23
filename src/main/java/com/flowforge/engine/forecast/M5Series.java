package com.flowforge.engine.forecast;

public record M5Series(
        String id,
        String itemId,
        String deptId,
        String catId,
        String storeId,
        String stateId,
        double[] sales
) {
}
