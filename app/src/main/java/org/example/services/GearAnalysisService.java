package org.example.services;

import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.example.App;
import org.example.GearStats;
import org.example.RideData;
import org.example.UserConfig;

public class GearAnalysisService {
    public Map<String, Object> processFitFile(File fitFile) {
        try {
            // Process FIT file
            App.processFitFile(fitFile);
            List<RideData> rideRecords = App.getRideRecords();

            if (rideRecords.isEmpty()) {
                return Map.of("error", "No ride data found in the FIT file.");
            }

            UserConfig defaultConfig = new UserConfig(null, null, "12Shimano34", 53, 39);
            Map<String, GearStats> gearStatsMap = analyzeGearUsage(defaultConfig, rideRecords);

            // Prepare formatted output
            List<Map<String, Object>> formattedGearData = formatGearData(gearStatsMap, defaultConfig);

            Map<String, Object> result = new HashMap<>();
            result.put("status", "success");
            result.put("message", "File processed successfully");
            result.put("gears_used", gearStatsMap.size());
            result.put("gear_analysis", formattedGearData);
            
            return result;
        } catch (Exception e) {
            e.printStackTrace();
            return Map.of("error", "Failed to process the FIT file");
        }
    }

    private Map<String, GearStats> analyzeGearUsage(UserConfig config, List<RideData> rideRecords) {
        Map<String, GearStats> gearStatsMap = new HashMap<>();

        for (RideData ride : rideRecords) {
            if (config.getMinCadence() != null && ride.getCadence() < config.getMinCadence()) {
                continue;
            }
            if (config.getMinPower() != null && ride.getPower() < config.getMinPower()) {
                continue;
            }
            String gearKey = ride.getFrontGear() + ":" + ride.getRearGear();
            gearStatsMap.putIfAbsent(gearKey, new GearStats());
            GearStats stats = gearStatsMap.get(gearKey);
            stats.addRideData(ride);
        }
        return gearStatsMap;
    }

    private List<Map<String, Object>> formatGearData(Map<String, GearStats> gearStatsMap, UserConfig config) {
        List<Map<String, Object>> formattedGears = new ArrayList<>();

        int totalRideTime = gearStatsMap.values().stream()
                .mapToInt(stats -> stats.getTotalTimeSeconds()).sum();

        int[] cassetteTeeth = org.example.CassetteData.getCassette(config.getCassette());
        if (cassetteTeeth == null || cassetteTeeth.length == 0) {
            System.err.println("⚠️ Warning: Cassette data is empty or null. Defaulting to empty array.");
            cassetteTeeth = new int[0]; // Ensure it's at least an empty array
        }

        for (Map.Entry<String, GearStats> entry : gearStatsMap.entrySet()) {
            String gear = entry.getKey();
            GearStats stats = entry.getValue();
            int[] gearNumbers = extractGearNumbers(gear);
            int frontIndex = gearNumbers[0] - 1;
            int rearIndex = gearNumbers[1] - 1;
            int frontTeeth = (frontIndex == 0) ? config.getSmallChainring() : config.getBigChainring();
            int rearTeeth = (rearIndex >= 0 && rearIndex < cassetteTeeth.length) ? cassetteTeeth[rearIndex] : 0;
            double gearRatio = (rearTeeth > 0) ? (double) frontTeeth / rearTeeth : 0;
            double avgSpeed = stats.getNumRecords() > 0 ? stats.getTotalSpeed().doubleValue() / stats.getNumRecords() : 0;
            double avgCadence = stats.getNumRecords() > 0 ? (double) stats.getTotalCadence() / stats.getNumRecords() : 0;
            double avgPower = stats.getNumRecords() > 0 ? (double) stats.getTotalPower() / stats.getNumRecords() : 0;
            double usagePercentage = (totalRideTime > 0) ? (stats.getTotalTimeSeconds() * 100.0) / totalRideTime : 0;
            
            String zone = classifyGearZone(rearTeeth, cassetteTeeth);

            // Format output
            Map<String, Object> formattedGear = new HashMap<>();
            formattedGear.put("gear", gear);
            formattedGear.put("front_teeth", frontTeeth);
            formattedGear.put("rear_teeth", rearTeeth);
            formattedGear.put("gear_ratio", String.format("%.2f", gearRatio));
            formattedGear.put("zone", zone);
            formattedGear.put("total_time", formatTime(stats.getTotalTimeSeconds()));
            formattedGear.put("usage_percentage", String.format("%.1f%%", usagePercentage));
            formattedGear.put("avg_speed", String.format("%.2f km/h", avgSpeed));
            formattedGear.put("avg_cadence", String.format("%.1f", avgCadence));
            formattedGear.put("avg_power", String.format("%.1f W", avgPower));

            formattedGears.add(formattedGear);
        }

        // Sort gears logically (e.g., 2:1, 2:2, 2:3, etc.)
        formattedGears.sort(Comparator.comparing(g -> (String) g.get("gear")));
        
        return formattedGears;
    }

    private int[] extractGearNumbers(String gearKey) {
        String[] parts = gearKey.split(":");
        return new int[]{Integer.parseInt(parts[0]), Integer.parseInt(parts[1])};
    }

    private String formatTime(int seconds) {
        int minutes = seconds / 60;
        int remainingSeconds = seconds % 60;
        return minutes > 0 ? String.format("%dm %ds", minutes, remainingSeconds) : String.format("%ds", remainingSeconds);
    }

    private String classifyGearZone(int rearTeeth, int[] cassetteTeeth) {
        if (cassetteTeeth.length == 0) {
            System.err.println("⚠️ Warning: Cassette data is empty! Cannot classify gear zone.");
            return "Unknown Zone";  // 🚨 Return a safe value to prevent crashes
        }
    
        int len = cassetteTeeth.length;
    
        if (rearTeeth == cassetteTeeth[0] || rearTeeth == cassetteTeeth[1] ||
            rearTeeth == cassetteTeeth[len - 1] || rearTeeth == cassetteTeeth[len - 2]) {
            return "🔴 Red Zone";
        } else if (rearTeeth == cassetteTeeth[2] || rearTeeth == cassetteTeeth[len - 3]) {
            return "🟠 Orange Zone";
        }
        return "🟢 Green Zone";
    }
    
}
