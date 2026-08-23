package com.flowforge.domain;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

/**
 * Mutable snapshot of the 12-node / 60-region Scottish fulfillment network.
 */
public final class FulfillmentNetwork {

    private final Map<String, NetworkNode> nodes = new LinkedHashMap<>();
    private final Map<String, DemandRegion> regions = new LinkedHashMap<>();
    private final Map<String, Lane> lanes = new LinkedHashMap<>();

    public void addNode(NetworkNode node) {
        nodes.put(node.id(), node);
    }

    public void addRegion(DemandRegion region) {
        regions.put(region.id(), region);
    }

    public void addLane(Lane lane) {
        lanes.put(lane.id(), lane);
    }

    public NetworkNode node(String id) {
        NetworkNode node = nodes.get(id);
        if (node == null) {
            throw new NoSuchElementException("Unknown node: " + id);
        }
        return node;
    }

    public DemandRegion region(String id) {
        DemandRegion region = regions.get(id);
        if (region == null) {
            throw new NoSuchElementException("Unknown region: " + id);
        }
        return region;
    }

    public Lane lane(String id) {
        Lane lane = lanes.get(id);
        if (lane == null) {
            throw new NoSuchElementException("Unknown lane: " + id);
        }
        return lane;
    }

    public Collection<NetworkNode> nodes() {
        return nodes.values();
    }

    public List<NetworkNode> fulfillmentNodes() {
        return nodes.values().stream().filter(NetworkNode::isFulfillment).collect(Collectors.toList());
    }

    public Collection<DemandRegion> regions() {
        return regions.values();
    }

    public Collection<Lane> lanes() {
        return lanes.values();
    }

    public void replaceNode(NetworkNode node) {
        nodes.put(node.id(), node);
    }

    public void replaceRegion(DemandRegion region) {
        regions.put(region.id(), region);
    }

    public void replaceLane(Lane lane) {
        lanes.put(lane.id(), lane);
    }

    public FulfillmentNetwork copy() {
        FulfillmentNetwork copy = new FulfillmentNetwork();
        nodes.values().forEach(copy::addNode);
        regions.values().forEach(copy::addRegion);
        lanes.values().forEach(copy::addLane);
        return copy;
    }

    public List<Lane> outgoing(String fromId) {
        List<Lane> out = new ArrayList<>();
        for (Lane lane : lanes.values()) {
            if (lane.fromId().equals(fromId)) {
                out.add(lane);
            }
        }
        return out;
    }

    public GeoPoint locationOf(String id) {
        if (nodes.containsKey(id)) {
            return nodes.get(id).location();
        }
        if (regions.containsKey(id)) {
            return regions.get(id).location();
        }
        throw new NoSuchElementException("Unknown location: " + id);
    }
}
