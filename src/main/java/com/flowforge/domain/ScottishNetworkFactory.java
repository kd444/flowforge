package com.flowforge.domain;

import java.util.List;

/**
 * Seeds the live 12-node Scottish fulfillment network and 60 demand regions.
 */
public final class ScottishNetworkFactory {

    private ScottishNetworkFactory() {
    }

    public static FulfillmentNetwork build() {
        FulfillmentNetwork network = new FulfillmentNetwork();
        seedNodes(network);
        seedRegions(network);
        seedLanes(network);
        return network;
    }

    private static void seedNodes(FulfillmentNetwork network) {
        add(network, "GLW-NDC", "Glasgow National DC", NodeType.NATIONAL_DC, 55.8642, -4.2518, 48_000, 0.11, 2_400, 18_000);
        add(network, "EDI-RDC", "Edinburgh Regional DC", NodeType.REGIONAL_DC, 55.9533, -3.1883, 22_000, 0.14, 1_200, 8_400);
        add(network, "ABD-RDC", "Aberdeen Regional DC", NodeType.REGIONAL_DC, 57.1497, -2.0943, 16_000, 0.16, 900, 5_200);
        add(network, "INV-RDC", "Inverness Regional DC", NodeType.REGIONAL_DC, 57.4778, -4.2247, 12_000, 0.18, 700, 3_600);
        add(network, "DUN-FC", "Dundee Fulfillment", NodeType.FULFILLMENT_CENTER, 56.4620, -2.9707, 7_500, 0.21, 380, 2_100);
        add(network, "STI-FC", "Stirling Fulfillment", NodeType.FULFILLMENT_CENTER, 56.1165, -3.9369, 6_800, 0.20, 320, 1_800);
        add(network, "LIV-FC", "Livingston Fulfillment", NodeType.FULFILLMENT_CENTER, 55.8864, -3.5226, 7_200, 0.19, 340, 2_000);
        add(network, "MOT-FC", "Motherwell Fulfillment", NodeType.FULFILLMENT_CENTER, 55.7890, -3.9968, 8_000, 0.18, 360, 2_400);
        add(network, "PAI-FC", "Paisley Fulfillment", NodeType.FULFILLMENT_CENTER, 55.8456, -4.4239, 6_400, 0.20, 300, 1_700);
        add(network, "FAL-FC", "Falkirk Fulfillment", NodeType.FULFILLMENT_CENTER, 56.0019, -3.7839, 6_200, 0.20, 280, 1_600);
        add(network, "KIL-FC", "Kilmarnock Fulfillment", NodeType.FULFILLMENT_CENTER, 55.6117, -4.4957, 5_800, 0.22, 260, 1_400);
        add(network, "PER-FC", "Perth Fulfillment", NodeType.FULFILLMENT_CENTER, 56.3950, -3.4308, 6_000, 0.21, 270, 1_500);

        add(network, "HUB-GRG", "Grangemouth Transfer", NodeType.TRANSFER_HUB, 56.0120, -3.7160, 0, 0.0, 0, 0);
        add(network, "HUB-COA", "Coatbridge Transfer", NodeType.TRANSFER_HUB, 55.8620, -4.0270, 0, 0.0, 0, 0);
        add(network, "HUB-PER", "Perth Junction", NodeType.TRANSFER_HUB, 56.3920, -3.4390, 0, 0.0, 0, 0);
        add(network, "HUB-AVI", "Aviemore Junction", NodeType.TRANSFER_HUB, 57.1890, -3.8280, 0, 0.0, 0, 0);
        add(network, "HUB-OBN", "Oban Gateway", NodeType.TRANSFER_HUB, 56.4150, -5.4710, 0, 0.0, 0, 0);
        add(network, "HUB-KYL", "Kyle of Lochalsh", NodeType.TRANSFER_HUB, 57.2790, -5.7140, 0, 0.0, 0, 0);
        add(network, "HUB-SCR", "Scrabster Gateway", NodeType.TRANSFER_HUB, 58.6100, -3.5500, 0, 0.0, 0, 0);
        add(network, "HUB-ABD", "Aberdeen Harbour", NodeType.TRANSFER_HUB, 57.1440, -2.0780, 0, 0.0, 0, 0);
    }

    private static void seedRegions(FulfillmentNetwork network) {
        Object[][] rows = {
                {"R01", "Glasgow Central", 55.8600, -4.2570, "GLW-NDC", 1.80},
                {"R02", "Glasgow South", 55.8280, -4.2870, "PAI-FC", 1.35},
                {"R03", "Glasgow North", 55.8920, -4.2800, "GLW-NDC", 1.25},
                {"R04", "Edinburgh Central", 55.9530, -3.1890, "EDI-RDC", 1.70},
                {"R05", "Edinburgh East", 55.9500, -3.1100, "EDI-RDC", 1.05},
                {"R06", "Edinburgh West", 55.9420, -3.2920, "LIV-FC", 1.10},
                {"R07", "Aberdeen City", 57.1500, -2.1100, "ABD-RDC", 1.20},
                {"R08", "Aberdeenshire", 57.2800, -2.3200, "ABD-RDC", 0.85},
                {"R09", "Inverness", 57.4780, -4.2250, "INV-RDC", 0.90},
                {"R10", "Highland North", 58.4400, -4.4200, "INV-RDC", 0.40},
                {"R11", "Dundee", 56.4620, -2.9670, "DUN-FC", 1.05},
                {"R12", "Angus", 56.7000, -2.6600, "DUN-FC", 0.55},
                {"R13", "Stirling", 56.1190, -3.9360, "STI-FC", 0.75},
                {"R14", "Falkirk", 56.0010, -3.7840, "FAL-FC", 0.80},
                {"R15", "West Lothian", 55.8900, -3.5500, "LIV-FC", 0.95},
                {"R16", "Midlothian", 55.8500, -3.0800, "EDI-RDC", 0.70},
                {"R17", "East Lothian", 55.9500, -2.7700, "EDI-RDC", 0.60},
                {"R18", "Fife East", 56.3400, -2.8000, "DUN-FC", 0.72},
                {"R19", "Fife West", 56.0700, -3.4400, "FAL-FC", 0.88},
                {"R20", "Perth", 56.3960, -3.4300, "PER-FC", 0.78},
                {"R21", "Kinross", 56.2050, -3.4210, "PER-FC", 0.35},
                {"R22", "Paisley", 55.8470, -4.4270, "PAI-FC", 0.82},
                {"R23", "Renfrewshire", 55.8300, -4.5000, "PAI-FC", 0.74},
                {"R24", "Inverclyde", 55.9420, -4.7610, "PAI-FC", 0.48},
                {"R25", "North Lanarkshire", 55.8290, -3.9820, "MOT-FC", 1.15},
                {"R26", "South Lanarkshire", 55.6740, -3.7820, "MOT-FC", 1.00},
                {"R27", "East Dunbartonshire", 55.9400, -4.2100, "GLW-NDC", 0.62},
                {"R28", "West Dunbartonshire", 55.9430, -4.5660, "PAI-FC", 0.50},
                {"R29", "East Renfrewshire", 55.7640, -4.3360, "PAI-FC", 0.55},
                {"R30", "North Ayrshire", 55.6400, -4.7600, "KIL-FC", 0.68},
                {"R31", "South Ayrshire", 55.4580, -4.6290, "KIL-FC", 0.58},
                {"R32", "East Ayrshire", 55.5110, -4.2890, "KIL-FC", 0.60},
                {"R33", "Dumfries", 55.0700, -3.6110, "MOT-FC", 0.52},
                {"R34", "Galloway", 54.9000, -4.4000, "KIL-FC", 0.32},
                {"R35", "Scottish Borders", 55.5480, -2.7860, "EDI-RDC", 0.50},
                {"R36", "Argyll", 56.2300, -5.4300, "HUB-OBN", 0.38},
                {"R37", "Bute", 55.8360, -5.0550, "PAI-FC", 0.18},
                {"R38", "Moray", 57.6100, -3.3100, "INV-RDC", 0.48},
                {"R39", "Orkney", 58.9810, -2.9600, "HUB-SCR", 0.22},
                {"R40", "Shetland", 60.1550, -1.1490, "HUB-ABD", 0.20},
                {"R41", "Western Isles", 58.2100, -6.3900, "HUB-KYL", 0.24},
                {"R42", "Clackmannanshire", 56.1150, -3.7950, "STI-FC", 0.36},
                {"R43", "Helensburgh", 56.0040, -4.7330, "PAI-FC", 0.28},
                {"R44", "Cumbernauld", 55.9460, -3.9880, "MOT-FC", 0.66},
                {"R45", "Hamilton", 55.7770, -4.0320, "MOT-FC", 0.70},
                {"R46", "Ayr", 55.4580, -4.6290, "KIL-FC", 0.54},
                {"R47", "Kilmarnock", 55.6110, -4.4980, "KIL-FC", 0.58},
                {"R48", "Greenock", 55.9480, -4.7610, "PAI-FC", 0.46},
                {"R49", "Dunfermline", 56.0720, -3.4390, "FAL-FC", 0.72},
                {"R50", "Kirkcaldy", 56.1100, -3.1600, "DUN-FC", 0.64},
                {"R51", "Livingston Town", 55.8880, -3.5230, "LIV-FC", 0.76},
                {"R52", "Bathgate", 55.9020, -3.6440, "LIV-FC", 0.42},
                {"R53", "Musselburgh", 55.9420, -3.0540, "EDI-RDC", 0.40},
                {"R54", "Dalkeith", 55.8930, -3.0700, "EDI-RDC", 0.38},
                {"R55", "St Andrews", 56.3390, -2.7960, "DUN-FC", 0.34},
                {"R56", "Oban", 56.4150, -5.4730, "HUB-OBN", 0.26},
                {"R57", "Fort William", 56.8200, -5.1050, "INV-RDC", 0.30},
                {"R58", "Elgin", 57.6490, -3.3150, "INV-RDC", 0.40},
                {"R59", "Peterhead", 57.5050, -1.7830, "ABD-RDC", 0.36},
                {"R60", "Fraserburgh", 57.6930, -2.0050, "ABD-RDC", 0.28}
        };
        for (Object[] row : rows) {
            network.addRegion(new DemandRegion(
                    (String) row[0],
                    (String) row[1],
                    new GeoPoint((Double) row[2], (Double) row[3]),
                    (String) row[4],
                    (Double) row[5]
            ));
        }
    }

    private static void seedLanes(FulfillmentNetwork network) {
        List<String> trunk = List.of(
                "GLW-NDC", "EDI-RDC", "ABD-RDC", "INV-RDC",
                "DUN-FC", "STI-FC", "LIV-FC", "MOT-FC",
                "PAI-FC", "FAL-FC", "KIL-FC", "PER-FC"
        );
        for (int i = 0; i < trunk.size(); i++) {
            for (int j = 0; j < trunk.size(); j++) {
                if (i == j) {
                    continue;
                }
                connect(network, trunk.get(i), trunk.get(j), TransportMode.TRUNK_ROAD, 1800, 4.0);
            }
        }

        rail(network, "GLW-NDC", "EDI-RDC", 2400);
        rail(network, "GLW-NDC", "STI-FC", 1400);
        rail(network, "STI-FC", "PER-FC", 1200);
        rail(network, "PER-FC", "DUN-FC", 1100);
        rail(network, "DUN-FC", "ABD-RDC", 1600);
        rail(network, "PER-FC", "INV-RDC", 900);
        rail(network, "GLW-NDC", "MOT-FC", 1600);
        rail(network, "EDI-RDC", "LIV-FC", 1300);
        rail(network, "FAL-FC", "EDI-RDC", 1200);

        connect(network, "GLW-NDC", "HUB-COA", TransportMode.TRUNK_ROAD, 4000, 0);
        connect(network, "HUB-COA", "MOT-FC", TransportMode.TRUNK_ROAD, 4000, 0);
        connect(network, "FAL-FC", "HUB-GRG", TransportMode.TRUNK_ROAD, 4000, 0);
        connect(network, "HUB-GRG", "EDI-RDC", TransportMode.RAIL, 2200, 0);
        connect(network, "PER-FC", "HUB-PER", TransportMode.TRUNK_ROAD, 3000, 0);
        connect(network, "HUB-PER", "INV-RDC", TransportMode.RAIL, 800, 0);
        connect(network, "INV-RDC", "HUB-AVI", TransportMode.TRUNK_ROAD, 1400, 0);
        connect(network, "HUB-AVI", "ABD-RDC", TransportMode.TRUNK_ROAD, 900, 0);
        connect(network, "STI-FC", "HUB-OBN", TransportMode.TRUNK_ROAD, 600, 8);
        connect(network, "INV-RDC", "HUB-KYL", TransportMode.TRUNK_ROAD, 500, 6);
        connect(network, "INV-RDC", "HUB-SCR", TransportMode.TRUNK_ROAD, 500, 10);
        connect(network, "ABD-RDC", "HUB-ABD", TransportMode.TRUNK_ROAD, 2500, 0);

        connect(network, "GLW-NDC", "HUB-ABD", TransportMode.AIR, 180, 40);
        connect(network, "EDI-RDC", "HUB-ABD", TransportMode.AIR, 160, 40);
        connect(network, "ABD-RDC", "R40", TransportMode.AIR, 80, 55);
        connect(network, "HUB-ABD", "R40", TransportMode.AIR, 90, 55);
        connect(network, "HUB-SCR", "R39", TransportMode.FERRY, 120, 18);
        connect(network, "ABD-RDC", "R39", TransportMode.FERRY, 90, 22);
        connect(network, "HUB-KYL", "R41", TransportMode.FERRY, 110, 16);
        connect(network, "HUB-OBN", "R41", TransportMode.FERRY, 80, 20);
        connect(network, "HUB-OBN", "R36", TransportMode.LAST_MILE, 400, 0);
        connect(network, "HUB-OBN", "R56", TransportMode.LAST_MILE, 400, 0);

        for (DemandRegion region : network.regions()) {
            if (isIsland(region.id())) {
                continue;
            }
            NetworkNode nearest = nearestFulfillment(network, region);
            connect(network, nearest.id(), region.id(), TransportMode.LAST_MILE, 900, 0);
            if (!"GLW-NDC".equals(nearest.id())
                    && nearest.location().haversineKm(region.location()) < 140) {
                connect(network, "GLW-NDC", region.id(), TransportMode.LAST_MILE, 400, 2);
            }
        }
    }

    private static boolean isIsland(String regionId) {
        return "R39".equals(regionId) || "R40".equals(regionId) || "R41".equals(regionId);
    }

    private static NetworkNode nearestFulfillment(FulfillmentNetwork network, DemandRegion region) {
        NetworkNode best = null;
        double bestKm = Double.MAX_VALUE;
        for (NetworkNode node : network.fulfillmentNodes()) {
            double km = node.location().haversineKm(region.location());
            if (km < bestKm) {
                bestKm = km;
                best = node;
            }
        }
        return best;
    }

    private static void rail(FulfillmentNetwork network, String a, String b, int cap) {
        connect(network, a, b, TransportMode.RAIL, cap, 0);
        connect(network, b, a, TransportMode.RAIL, cap, 0);
    }

    private static void connect(
            FulfillmentNetwork network,
            String from,
            String to,
            TransportMode mode,
            int capacity,
            double toll
    ) {
        GeoPoint a = network.locationOf(from);
        GeoPoint b = network.locationOf(to);
        double km = Math.max(4.0, a.haversineKm(b));
        if (mode == TransportMode.FERRY) {
            km *= 1.15;
        }
        if (mode == TransportMode.AIR) {
            km *= 1.05;
        }
        network.addLane(new Lane(Lane.idOf(from, to, mode), from, to, mode, km, capacity, toll));
    }

    private static void add(
            FulfillmentNetwork network,
            String id,
            String name,
            NodeType type,
            double lat,
            double lon,
            int cap,
            double hold,
            int safety,
            int onHand
    ) {
        network.addNode(new NetworkNode(id, name, type, new GeoPoint(lat, lon), cap, hold, safety, onHand));
    }
}
