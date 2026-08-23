package com.flowforge.engine.agent;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public final class NetworkProposal {
    private String rationale = "";
    private Map<String, Integer> safetyStockDeltas = new LinkedHashMap<>();
    private Map<String, Integer> laneCapacityDeltas = new LinkedHashMap<>();
    private List<Assignment> assignments = new ArrayList<>();

    public record Assignment(String nodeId, String regionId, int priority) {
    }

    public String rationale() {
        return rationale;
    }

    public String getRationale() {
        return rationale;
    }

    public void setRationale(String rationale) {
        this.rationale = rationale == null ? "" : rationale;
    }

    public Map<String, Integer> safetyStockDeltas() {
        return safetyStockDeltas;
    }

    public Map<String, Integer> getSafetyStockDeltas() {
        return safetyStockDeltas;
    }

    public void setSafetyStockDeltas(Map<String, Integer> safetyStockDeltas) {
        this.safetyStockDeltas = safetyStockDeltas == null ? new LinkedHashMap<>() : safetyStockDeltas;
    }

    public Map<String, Integer> laneCapacityDeltas() {
        return laneCapacityDeltas;
    }

    public Map<String, Integer> getLaneCapacityDeltas() {
        return laneCapacityDeltas;
    }

    public void setLaneCapacityDeltas(Map<String, Integer> laneCapacityDeltas) {
        this.laneCapacityDeltas = laneCapacityDeltas == null ? new LinkedHashMap<>() : laneCapacityDeltas;
    }

    public List<Assignment> assignments() {
        return assignments;
    }

    public List<Assignment> getAssignments() {
        return assignments;
    }

    public void setAssignments(List<Assignment> assignments) {
        this.assignments = assignments == null ? new ArrayList<>() : assignments;
    }
}
