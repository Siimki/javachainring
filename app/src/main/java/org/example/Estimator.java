package org.example;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Estimator {

    public String estimateOptimalSingleChainring(RideSession session, UserConfig config) {
        List<RideData> filtered = session.rideRecords.stream()
            .filter(ride -> config.getMinCadence() == null || ride.getCadence() >= config.getMinCadence())
            .filter(ride -> config.getMinPower() == null || ride.getPower() >= config.getMinPower())
            .toList();

        int[] cassetteTeeth = CassetteData.getCassette(config.getCassette());
        if (cassetteTeeth == null || cassetteTeeth.length == 0) return "❌ Invalid cassette config";

        int best = -1;
        int bestRed = Integer.MAX_VALUE;
        int bestGreen = -1;
        int bestOrange = Integer.MAX_VALUE;
        Map<Integer, Map<String, Integer>> allResults = new HashMap<>();

        for (int candidate = 30; candidate <= 80; candidate++) {
            Map<String, Integer> stats = simulateZoneTimes(filtered, cassetteTeeth, candidate, config);
            allResults.put(candidate, stats);
        
            int red = stats.get("red");
            int green = stats.get("green");
            int orange = stats.get("orange");
        
            if (
                red < bestRed ||
                (red == bestRed && green > bestGreen) ||
                (red == bestRed && green == bestGreen && orange < bestOrange)
            ) {
                bestRed = red;
                bestGreen = green;
                bestOrange = orange;
                best = candidate;
            }
        }

        System.out.println("\n🔧 Chainring Simulation Summary:");
        for (int chainring : allResults.keySet()) {
            Map<String, Integer> stats = allResults.get(chainring);
            int total = stats.get("total");
            System.out.printf(
                "➡️ %dT → 🔴 %ds (%.1f%%), 🟠 %ds (%.1f%%), 🟢 %ds (%.1f%%), Total: %ds%n",
                chainring,
                stats.get("red"), (stats.get("red") * 100.0 / total),
                stats.get("orange"), (stats.get("orange") * 100.0 / total),
                stats.get("green"), (stats.get("green") * 100.0 / total),
                total
            );
        }

        return "✅ Best estimated 1x chainring: " + best + "T";
    }

    private Map<String, Integer> simulateZoneTimes(List<RideData> data, int[] cassette, int candidate, UserConfig config) {
        int red = 0, orange = 0, green = 0, total = 0;

        for (RideData ride : data) {
            int rearIndex = ride.getRearGear() - 1;
            if (rearIndex < 0 || rearIndex >= cassette.length) continue;

            int frontTeeth = (ride.getFrontGear() == 1) ? config.getSmallChainring() : config.getBigChainring();
            double originalRatio = (double) frontTeeth / cassette[rearIndex];

            // Simulate which gear on candidate matches this ratio
            int closestIndex = 0;
            double smallestDiff = Double.MAX_VALUE;
            for (int i = 0; i < cassette.length; i++) {
                double simRatio = (double) candidate / cassette[i];
                double diff = Math.abs(simRatio - originalRatio);
                if (diff < smallestDiff) {
                    smallestDiff = diff;
                    closestIndex = i;
                }
            }

            String zone = classifyGearZone(cassette[closestIndex], cassette);
            switch (zone) {
                case "red" -> red++;
                case "orange" -> orange++;
                case "green" -> green++;
            }
            total++;
        }

        Map<String, Integer> result = new HashMap<>();
        result.put("red", red);
        result.put("orange", orange);
        result.put("green", green);
        result.put("total", total);
        return result;
    }

    private String classifyGearZone(int rearTeeth, int[] cassette) {
        int i = -1;
        for (int j = 0; j < cassette.length; j++) {
            if (cassette[j] == rearTeeth) {
                i = j;
                break;
            }
        }
        if (i == -1) return "green";

        int len = cassette.length;
        if (i == 0 || i == 1 || i == len - 1) return "red";
        if (i == 2 || i == len - 3 || i == len - 2) return "orange";
        return "green";
    }
}
