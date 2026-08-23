package com.flowforge.api.dto;

import jakarta.validation.constraints.NotBlank;

public class RouteRequestBody {

    @NotBlank
    private String regionId;
    private Integer departHour = 9;
    private Integer slaHours = 24;
    private String algorithm = "AUTO";
    private boolean requireStock = true;

    public String getRegionId() {
        return regionId;
    }

    public void setRegionId(String regionId) {
        this.regionId = regionId;
    }

    public Integer getDepartHour() {
        return departHour;
    }

    public void setDepartHour(Integer departHour) {
        this.departHour = departHour;
    }

    public Integer getSlaHours() {
        return slaHours;
    }

    public void setSlaHours(Integer slaHours) {
        this.slaHours = slaHours;
    }

    public String getAlgorithm() {
        return algorithm;
    }

    public void setAlgorithm(String algorithm) {
        this.algorithm = algorithm;
    }

    public boolean isRequireStock() {
        return requireStock;
    }

    public void setRequireStock(boolean requireStock) {
        this.requireStock = requireStock;
    }
}
