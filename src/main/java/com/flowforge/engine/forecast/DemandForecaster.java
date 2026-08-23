package com.flowforge.engine.forecast;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Lightweight seasonal forecast: Croston for intermittent SKUs, seasonal naive
 * + damped trend for regular M5-like series.
 */
public final class DemandForecaster {

    public record Forecast(double[] horizon, double dailyMean, String method) {
    }

    private DemandForecaster() {
    }

    public static Forecast forecast(double[] history, int horizonDays) {
        if (history == null || history.length < 14) {
            double[] zeros = new double[horizonDays];
            return new Forecast(zeros, 0, "insufficient");
        }
        double zeroShare = 0;
        for (double v : history) {
            if (v <= 0) {
                zeroShare++;
            }
        }
        zeroShare /= history.length;
        if (zeroShare > 0.35) {
            return croston(history, horizonDays);
        }
        return seasonalNaive(history, horizonDays);
    }

    public static Map<String, Double> regionDailyMean(
            List<M5Series> catalog,
            Map<String, Double> regionScale,
            int lookbackDays
    ) {
        Map<String, Double> storeMean = new HashMap<>();
        Map<String, Integer> storeCount = new HashMap<>();
        for (M5Series series : catalog) {
            int n = series.sales().length;
            int from = Math.max(0, n - lookbackDays);
            double sum = 0;
            int count = 0;
            for (int i = from; i < n; i++) {
                sum += series.sales()[i];
                count++;
            }
            double mean = count == 0 ? 0 : sum / count;
            storeMean.merge(series.storeId(), mean, Double::sum);
            storeCount.merge(series.storeId(), 1, Integer::sum);
        }
        Map<String, Double> regionForecast = new HashMap<>();
        double catalogMean = storeMean.values().stream().mapToDouble(Double::doubleValue).average().orElse(20.0);
        regionScale.forEach((regionId, scale) -> regionForecast.put(regionId, Math.max(1.0, catalogMean * scale * 0.18)));
        return regionForecast;
    }

    private static Forecast seasonalNaive(double[] history, int horizonDays) {
        int season = 7;
        double[] horizon = new double[horizonDays];
        int n = history.length;
        double lastWeek = 0;
        double prevWeek = 0;
        for (int i = 0; i < season; i++) {
            lastWeek += history[n - season + i];
            prevWeek += history[n - 2 * season + i];
        }
        double trend = (lastWeek - prevWeek) / season;
        for (int h = 0; h < horizonDays; h++) {
            double seasonal = history[n - season + (h % season)];
            horizon[h] = Math.max(0, seasonal + 0.35 * trend);
        }
        double mean = Arrays.stream(horizon).average().orElse(0);
        return new Forecast(horizon, mean, "seasonal-naive");
    }

    private static Forecast croston(double[] history, int horizonDays) {
        double alpha = 0.15;
        double z = 0;
        double p = 1;
        int since = 1;
        boolean seen = false;
        for (double demand : history) {
            if (demand > 0) {
                if (!seen) {
                    z = demand;
                    p = since;
                    seen = true;
                } else {
                    z = z + alpha * (demand - z);
                    p = p + alpha * (since - p);
                }
                since = 1;
            } else {
                since++;
            }
        }
        double rate = seen ? z / Math.max(1.0, p) : 0;
        double[] horizon = new double[horizonDays];
        Arrays.fill(horizon, rate);
        return new Forecast(horizon, rate, "croston");
    }
}
