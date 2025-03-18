package org.example;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.garmin.fit.Decode;
import com.garmin.fit.Mesg;
import com.garmin.fit.MesgBroadcaster;
import com.garmin.fit.MesgListener;

public class App {

    private static final List<RideData> rideRecords = new ArrayList<>();
    private static Short currentRearGear = 0;
    private static Short currentFrontGear = 2; //Assume it is on the big chainring

    public static void main(String[] args) {
        if (args.length < 1) {
            printUsage();
            return;
        }

        File fitFile = new File(args[0]);
        System.out.println("Attempting to open file: " + fitFile.getAbsolutePath());

        if (!fitFile.exists()) {
            System.err.println("Error: FIT file not found!");
            return;
        }

        UserConfig userConfig = parseArguments(args);
        processFitFile(fitFile);
        Map<String, GearStats> gearStatsMap = analyzeGearUsage(userConfig);
        printGearUsage(gearStatsMap, userConfig);
        printDrivetrainInfo(userConfig);
        System.out.println("\n --- End of program! ---");
    }

    private static void printDrivetrainInfo(UserConfig userConfig) {
        int[] cassetteTeeth = CassetteData.getCassette(userConfig.getCassette());
        if (cassetteTeeth == null) {
            System.err.println("⚠️ Warning: Unknown cassette type: " + userConfig.getCassette());
        } else {
            System.out.println(" Cassette: " + Arrays.toString(cassetteTeeth)
                    + "\n Front chainrings: " + userConfig.getBigChainring() + ", " + userConfig.getSmallChainring());
        }
    }

    private static UserConfig parseArguments(String[] args) {
        Integer bigChainring = (args.length > 1) ? tryParseInt(args[1]) : null;
        Integer smallChainring = (args.length > 2) ? tryParseInt(args[2]) : null;
        Integer minCadence = (args.length > 4) ? tryParseInt(args[4]) : null;
        Integer minPower = (args.length > 5) ? tryParseInt(args[5]) : null;
        String cassette = (args.length > 3) ? args[3] : null;

        return new UserConfig(minCadence, minPower, cassette, bigChainring, smallChainring);
    }

    private static Integer tryParseInt(String value) {
        try {
            return Integer.valueOf(value);
        } catch (NumberFormatException e) {
            System.err.println("Warning: Invalid number '" + value + "' ignored.");
            return null;
        }
    }

    private static Map<String, GearStats> analyzeGearUsage(UserConfig config) {
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
            stats.totalSpeed = stats.totalSpeed.add(ride.getSpeed());
            stats.totalCadence += ride.getCadence();
            stats.totalPower += ride.getPower();
            stats.totalTimeSeconds++;
            stats.numRecords++;
        }
        return gearStatsMap;
    }

    //Clunky? 
    private static void printGearUsage(Map<String, GearStats> gearStatsMap, UserConfig config) {
        System.out.println("📊 Gear usage statistics:");

        List<Map.Entry<String, GearStats>> sortedGears = new ArrayList<>(gearStatsMap.entrySet());
        sortedGears.sort(Comparator.comparingInt(entry
                -> parseGear(entry.getKey())
        ));

        int totalRideTime = sortedGears.stream().mapToInt(entry -> entry.getValue().totalTimeSeconds).sum();
        int[] cassetteTeeth = CassetteData.getCassette(config.getCassette());
        int totalRedTime = 0, totalOrangeTime = 0, totalGreenTime = 0, totalUnknownTime = 0;

        if (cassetteTeeth == null) {
            System.err.println("Warning: Unknown casseette type: " + config.getCassette() + "Gear ratios are incorrect!");
            cassetteTeeth = new int[0];
        }

        for (var entry : sortedGears) {
            String gear = entry.getKey();
            GearStats stats = entry.getValue();
            int[] gearNumbers = extractGearNumbers(gear);
            int frontIndex = gearNumbers[0] - 1; // Convert 1-based to 0-based index
            int rearIndex = gearNumbers[1] - 1; // Convert 1-based to 0-based index
            int frontTeeth = (frontIndex == 0) ? config.getSmallChainring() : config.getBigChainring();
            int rearTeeth = (rearIndex >= 0 && rearIndex < cassetteTeeth.length) ? cassetteTeeth[rearIndex] : 0;
            double gearRatio = (rearTeeth > 0) ? (double) frontTeeth / rearTeeth : 0;

            if (rearIndex >= cassetteTeeth.length || rearTeeth <= 0) {  // Out of bounds or invalid gear
                totalUnknownTime += stats.totalTimeSeconds;
                printUnknownGear(frontTeeth, stats, totalRideTime);
                continue;
            }
            //Write Green, Orange, Red conclusion.
            String zone = classifyGearZone(rearTeeth, cassetteTeeth);
            switch (zone) {
                case "🔴 Red Zone" ->
                    totalRedTime += stats.totalTimeSeconds;
                case "🟠 Orange Zone" ->
                    totalOrangeTime += stats.totalTimeSeconds;
                case "🟢 Green Zone" ->
                    totalGreenTime += stats.totalTimeSeconds;
            }

            printGearStats(gear, frontTeeth, rearTeeth, gearRatio, stats, totalRideTime);

        }
        printZoneSummary(totalRedTime, totalOrangeTime, totalGreenTime, totalRideTime);
    }

    private static void printUsage() {
        System.err.println("⚠️ Error: Please provide the FIT file path as an argument.");
        System.err.println("Usage: ./gradlew run --args=\"[big_chainring] [small_chainring] [cassette] [min_cadence] [min_power]\"");
        System.err.println("Example: ./gradlew run --args=\"53 39 12shimano34 80 200\"");
    }

    private static String classifyGearZone(int rearTeeth, int[] cassetteTeeth) {
        int len = cassetteTeeth.length;
        if (rearTeeth == cassetteTeeth[0] || rearTeeth == cassetteTeeth[1] || rearTeeth == cassetteTeeth[len - 1] || rearTeeth == cassetteTeeth[len - 2]) {
            return "🔴 Red Zone";
        } else if (rearTeeth == cassetteTeeth[2] || rearTeeth == cassetteTeeth[len - 3]) {
            return "🟠 Orange Zone";
        }
        return "🟢 Green Zone";
    }

    //Kinda bad readability. Should make the calculations and declare new variables. Not just do math inside print.
    private static void printUnknownGear(int frontTeeth, GearStats stats, int totalRideTime) {
        double usagePercentage = (stats.totalTimeSeconds * 100.0) / totalRideTime;
        System.out.printf("⚠️ Unknown Gear (%dT:??T) → ⏳ Time: %s sec (%.1f%%), 🚴 Avg Speed: %.2f km/h, Avg Cadence: %.1f, ⚡ Avg Power: %.1fW%n",
                frontTeeth, timeConvert(stats.totalTimeSeconds), usagePercentage,
                stats.totalSpeed.doubleValue() / stats.numRecords, (double) stats.totalCadence / stats.numRecords, (double) stats.totalPower / stats.numRecords);
    }

    private static void printGearStats(String gear, int frontTeeth, int rearTeeth, double gearRatio, GearStats stats, int totalRideTime) {
        double usagePercentage = (stats.totalTimeSeconds * 100.0) / totalRideTime;
        System.out.printf("⚙ Gear %s (%dT:%dT | Ratio: %.2f) → ⏳ Time: %s sec (%.1f%%), 🚴 Avg Speed: %.2f km/h,  Avg Cadence: %.1f, ⚡ Avg Power: %.1fW%n",
                gear, frontTeeth, rearTeeth, gearRatio, timeConvert(stats.totalTimeSeconds), usagePercentage,
                stats.totalSpeed.doubleValue() / stats.numRecords, (double) stats.totalCadence / stats.numRecords, (double) stats.totalPower / stats.numRecords);
    }

    private static void printZoneSummary(int totalRedTime, int totalOrangeTime, int totalGreenTime, int totalRideTime) {
        System.out.println("\n📊 Zone Summary:");
        System.out.printf("🔴 Red Zone: %s (%.1f%%)%n", timeConvert(totalRedTime), (totalRedTime * 100.0) / totalRideTime);
        System.out.printf("🟠 Orange Zone: %s (%.1f%%)%n", timeConvert(totalOrangeTime), (totalOrangeTime * 100.0) / totalRideTime);
        System.out.printf("🟢 Green Zone: %s (%.1f%%)%n", timeConvert(totalGreenTime), (totalGreenTime * 100.0) / totalRideTime);
    }

    private static int parseGear(String gearKey) {
        int[] gearNumbers = extractGearNumbers(gearKey);
        return (gearNumbers[0] * 100) + gearNumbers[1];
    }

    private static int[] extractGearNumbers(String gearKey) {
        String[] parts = gearKey.split(":");
        return new int[]{Integer.parseInt(parts[0]), Integer.parseInt(parts[1])};
    }

    private static String timeConvert(int seconds) {
        int minutes = seconds / 60;
        int remainingSeconds = seconds % 60;
        if (minutes > 0) {
            return String.format("%dm %ds", minutes, remainingSeconds);
        } else {
            return String.format("%ds", remainingSeconds);
        }
    }

    private static void printRideSummary() {
        System.out.println(" Stored ride data:");
        rideRecords.forEach(ride
                -> System.out.printf("⏱ Timestamp: %d, ⚙ Gear: %d:%d, Speed: %.2f km/h, Cadence: %d, ⚡ Power: %dW%n",
                        ride.getTimeStamp(), ride.getFrontGear(), ride.getRearGear(), ride.getSpeed().doubleValue(),
                        ride.getCadence(), ride.getPower())
        );
        System.out.println("\n Total records: " + rideRecords.size());
    }

    private static void processFitFile(File fitFile) {
        try (InputStream fitStream = new FileInputStream(fitFile)) {
            Decode decode = new Decode();
            MesgBroadcaster mesgBroadcaster = new MesgBroadcaster(decode);

            // Add listener for processing messages
            mesgBroadcaster.addListener(new MesgListener() {
                @Override
                public void onMesg(Mesg mesg) {
                    //   System.out.println("📩 Message received: " + mesg.getName());

                    switch (mesg.getName()) {
                        case "record" ->
                            handleRecordMessage(mesg);
                        case "event" ->
                            handleEventMessage(mesg);
                        case "session" ->
                            System.out.println(" Session info received.");
                        default -> {
                        } // Ignore other messages
                    }
                }
            });

            if (decode.read(fitStream, mesgBroadcaster)) {
                System.out.println(" FIT file successfully processed!");
            } else {
                System.err.println(" Error decoding FIT file.");
            }

        } catch (Exception e) {
            System.err.println("Error reading FIT file: " + e.getMessage());
        }
    }

    private static void handleRecordMessage(Mesg mesg) {
        RideData data = parseRecord(mesg); // front, back miss
        if (data != null) {
            rideRecords.add(data);
        }
    }

    private static void handleEventMessage(Mesg mesg) {

        for (var field : mesg.getFields()) {
            if (field.getName().equals("event")) {
                Short eventValue = (Short) field.getValue();
                String eventType = com.garmin.fit.Event.getByValue(eventValue).toString();
                //  System.out.println("Event detected" + eventType);
                if ("REAR_GEAR_CHANGE".equals(eventType)) {
                    updateRearGear(mesg);
                } else if ("FRONT_GEAR_CHANGE".equals(eventType)) {
                    updateFrontGear(mesg);
                }
            }
        }
    }

    private static void updateRearGear(Mesg mesg) {
        for (var field : mesg.getFields()) {
            if ("rear_gear_num".equals(field.getName())) {
                currentRearGear = (Short) field.getValue();
            }
        }
    }

    private static void updateFrontGear(Mesg mesg) {
        System.out.println("🚴 Front gear change detected!");
        for (var field : mesg.getFields()) {
            if ("front_gear_num".equals(field.getName())) {
                currentFrontGear = (Short) field.getValue();
                System.out.println("🔄 New Front Gear: " + currentFrontGear);
            }
        }
    }

    public static String getGreeting() {
        return "Hello, FIT World!";
    }

    private static RideData parseRecord(Mesg mesg) {
        long timestamp = getFieldLong(mesg, "timestamp", (int) 0L);
        //Lets see how this behaves. I changed it from speed to enhanced_speed. Some files had no speed field.  
        BigDecimal speed = getFieldBigDecimal(mesg, "enhanced_speed", BigDecimal.ZERO);
        int cadence = getFieldInt(mesg, "cadence", 0);
        int power = getFieldInt(mesg, "power", 0);

        // debug
        // mesg.getFields().forEach(field -> {
        //     System.out.println("Field Name: " + field.getName() + " → Value: " + field.getValue());
        // });
        return new RideData(timestamp, currentFrontGear, currentRearGear, speed, cadence, power);
    }

    private static int getFieldInt(Mesg mesg, String fieldName, int defaultValue) {
        Object value = mesg.getFieldValue(fieldName);
        return value instanceof Number ? ((Number) value).intValue() : defaultValue;
    }

    private static long getFieldLong(Mesg mesg, String fieldName, int defaultValue) {
        Object value = mesg.getFieldValue(fieldName);
        return value instanceof Number ? ((Number) value).longValue() : defaultValue;
    }

    private static BigDecimal getFieldBigDecimal(Mesg mesg, String fieldName, BigDecimal defaultValue) {
        Object value = mesg.getFieldValue(fieldName);
        return value != null ? new BigDecimal(value.toString()) : defaultValue;
    }

    public static String convertTimestamp(long fitTimestamp) {

        long FIT_EPOCH_OFFSET = 631065600L; // Garmin FIT epoch
        Instant instant = Instant.ofEpochSecond(FIT_EPOCH_OFFSET + fitTimestamp);
        ZonedDateTime localDateTime = instant.atZone(ZoneId.of("Asia/Tokyo")); // Japan or change to systemDefault()
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        return localDateTime.format(formatter);
    }

}
