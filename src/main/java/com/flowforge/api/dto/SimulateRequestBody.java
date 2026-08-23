package com.flowforge.api.dto;

public class SimulateRequestBody {
    private Integer days = 90;
    private Long seed = 42L;
    private boolean compareBaseline = true;

    public Integer getDays() {
        return days;
    }

    public void setDays(Integer days) {
        this.days = days;
    }

    public Long getSeed() {
        return seed;
    }

    public void setSeed(Long seed) {
        this.seed = seed;
    }

    public boolean isCompareBaseline() {
        return compareBaseline;
    }

    public void setCompareBaseline(boolean compareBaseline) {
        this.compareBaseline = compareBaseline;
    }
}
