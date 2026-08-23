package com.flowforge.engine.forecast;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Generates M5-shaped daily retail series (store / item / dept / cat / state).
 * Replace {@link #generate} with a CSV loader to train on the public M5 file.
 */
public final class M5SyntheticGenerator {

    public static final String[] CATEGORIES = {"FOODS", "HOUSEHOLD", "HOBBIES"};
    public static final String[] ITEMS = {
            "FOODS_1_001", "FOODS_1_014", "FOODS_2_207", "FOODS_3_586",
            "HOUSEHOLD_1_032", "HOUSEHOLD_2_101",
            "HOBBIES_1_008", "HOBBIES_2_044"
    };

    private M5SyntheticGenerator() {
    }

    public static List<M5Series> generate(List<String> storeIds, int days, long seed) {
        Random random = new Random(seed);
        List<M5Series> series = new ArrayList<>();
        for (String store : storeIds) {
            for (int i = 0; i < ITEMS.length; i++) {
                String item = ITEMS[i];
                String cat = item.substring(0, item.indexOf('_'));
                String dept = item.substring(0, item.lastIndexOf('_'));
                double[] sales = new double[days];
                double base = 6.0 + (i % 3) * 4.5 + random.nextDouble() * 3.0;
                if (store.startsWith("GLW") || store.startsWith("EDI")) {
                    base *= 1.45;
                }
                if (store.startsWith("INV") || store.startsWith("ABD")) {
                    base *= 0.72;
                }
                boolean intermittent = item.startsWith("HOBBIES");
                for (int d = 0; d < days; d++) {
                    double weekly = 1.0 + 0.18 * Math.sin(2 * Math.PI * ((d + 4) % 7) / 7.0);
                    double yearly = 1.0 + 0.22 * Math.sin(2 * Math.PI * d / 365.0 + 1.2);
                    double event = (d % 365 > 330) ? 1.35 : 1.0;
                    double noise = 1.0 + (random.nextGaussian() * 0.12);
                    double value = base * weekly * yearly * event * noise;
                    if (intermittent && random.nextDouble() < 0.45) {
                        value = 0;
                    }
                    sales[d] = Math.max(0, Math.round(value * 10.0) / 10.0);
                }
                series.add(new M5Series(
                        store + "_" + item,
                        item,
                        dept,
                        cat,
                        store,
                        "SCOTLAND",
                        sales
                ));
            }
        }
        return series;
    }
}
