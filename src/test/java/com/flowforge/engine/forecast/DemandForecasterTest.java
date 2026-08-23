package com.flowforge.engine.forecast;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DemandForecasterTest {

    @Test
    void seasonalNaiveKeepsWeeklyShape() {
        double[] history = new double[28];
        for (int i = 0; i < history.length; i++) {
            history[i] = 10 + (i % 7);
        }
        DemandForecaster.Forecast forecast = DemandForecaster.forecast(history, 7);
        assertEquals("seasonal-naive", forecast.method());
        assertEquals(10, forecast.horizon()[0], 1e-6);
        assertEquals(16, forecast.horizon()[6], 1e-6);
    }

    @Test
    void crostonHandlesIntermittentSeries() {
        double[] history = new double[30];
        history[3] = 8;
        history[11] = 6;
        history[20] = 10;
        DemandForecaster.Forecast forecast = DemandForecaster.forecast(history, 5);
        assertEquals("croston", forecast.method());
        assertTrue(forecast.dailyMean() > 0);
    }
}
