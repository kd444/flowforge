package com.flowforge.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "flowforge")
public class FlowForgeProperties {

    private final Redis redis = new Redis();
    private final Network network = new Network();
    private final Forecast forecast = new Forecast();
    private final Simulation simulation = new Simulation();
    private final Agent agent = new Agent();

    public Redis getRedis() {
        return redis;
    }

    public Network getNetwork() {
        return network;
    }

    public Forecast getForecast() {
        return forecast;
    }

    public Simulation getSimulation() {
        return simulation;
    }

    public Agent getAgent() {
        return agent;
    }

    public static class Redis {
        private boolean enabled;
        private String keyPrefix = "ff:";
        private long ttlSeconds = 3600;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getKeyPrefix() {
            return keyPrefix;
        }

        public void setKeyPrefix(String keyPrefix) {
            this.keyPrefix = keyPrefix;
        }

        public long getTtlSeconds() {
            return ttlSeconds;
        }

        public void setTtlSeconds(long ttlSeconds) {
            this.ttlSeconds = ttlSeconds;
        }
    }

    public static class Network {
        private int slaHoursDefault = 24;

        public int getSlaHoursDefault() {
            return slaHoursDefault;
        }

        public void setSlaHoursDefault(int slaHoursDefault) {
            this.slaHoursDefault = slaHoursDefault;
        }
    }

    public static class Forecast {
        private int historyDays = 420;
        private int horizonDays = 14;
        private int skuCount = 8;

        public int getHistoryDays() {
            return historyDays;
        }

        public void setHistoryDays(int historyDays) {
            this.historyDays = historyDays;
        }

        public int getHorizonDays() {
            return horizonDays;
        }

        public void setHorizonDays(int horizonDays) {
            this.horizonDays = horizonDays;
        }

        public int getSkuCount() {
            return skuCount;
        }

        public void setSkuCount(int skuCount) {
            this.skuCount = skuCount;
        }
    }

    public static class Simulation {
        private int defaultDays = 90;
        private long seed = 42;

        public int getDefaultDays() {
            return defaultDays;
        }

        public void setDefaultDays(int defaultDays) {
            this.defaultDays = defaultDays;
        }

        public long getSeed() {
            return seed;
        }

        public void setSeed(long seed) {
            this.seed = seed;
        }
    }

    public static class Agent {
        private String provider = "heuristic";
        private String openaiApiKey = "";
        private String openaiBaseUrl = "https://api.openai.com/v1";
        private String model = "gpt-4o-mini";
        private double maxFillDropPp = 2.0;
        private double maxCostIncreasePct = 5.0;

        public String getProvider() {
            return provider;
        }

        public void setProvider(String provider) {
            this.provider = provider;
        }

        public String getOpenaiApiKey() {
            return openaiApiKey;
        }

        public void setOpenaiApiKey(String openaiApiKey) {
            this.openaiApiKey = openaiApiKey;
        }

        public String getOpenaiBaseUrl() {
            return openaiBaseUrl;
        }

        public void setOpenaiBaseUrl(String openaiBaseUrl) {
            this.openaiBaseUrl = openaiBaseUrl;
        }

        public String getModel() {
            return model;
        }

        public void setModel(String model) {
            this.model = model;
        }

        public double getMaxFillDropPp() {
            return maxFillDropPp;
        }

        public void setMaxFillDropPp(double maxFillDropPp) {
            this.maxFillDropPp = maxFillDropPp;
        }

        public double getMaxCostIncreasePct() {
            return maxCostIncreasePct;
        }

        public void setMaxCostIncreasePct(double maxCostIncreasePct) {
            this.maxCostIncreasePct = maxCostIncreasePct;
        }
    }
}
