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
            System.err.println("Error: Please provide the FIT file path as an argument.");
            System.err.println("Usage: java -jar app.jar <path-to-fit-file>");
            return;
        }

        File fitFile = new File(args[0]);     
        System.out.println("Attempting to open file: " + fitFile.getAbsolutePath());
        System.out.println("Looking for FIT file at: " + fitFile.getAbsolutePath());

        if (!fitFile.exists()) {
            System.err.println("Error: FIT file not found!");
            return;
        }

        Integer minCadence = (args.length > 1) ? tryParseInt(args[1]) : null;
        Integer minPower = (args.length > 2) ? tryParseInt(args[2]) : null;

        processFitFile(fitFile);
       // printRideSummary();
        Map<String, GearStats> gearStatsMap = analyzeGearUsage(minCadence, minPower);
        printGearUsage(gearStatsMap);
        System.out.println("\n --- End of program! ---");
    }

    private static Integer tryParseInt(String value) {
        try {
            return Integer.valueOf(value);
        } catch (NumberFormatException e) {
            System.err.println("Warning: Invalid number '" + value + "' ignored.");
            return null;
        }
    }

    private static Map<String, GearStats> analyzeGearUsage(Integer minCadence, Integer minPower) {
        Map<String, GearStats> gearStatsMap = new HashMap<>();
        
        for (RideData ride : rideRecords) {

            if (minCadence != null && ride.getCadence() < minCadence) {
                continue;
            }
            if (minPower != null && ride.getPower() < minPower) {
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


    private static void printGearUsage(Map<String, GearStats> gearStatsMap) {
        System.out.println(" Gear usage:");
        List<Map.Entry<String, GearStats>> sortedGears = new ArrayList<>(gearStatsMap.entrySet());

        sortedGears.sort(Comparator.comparing((Map.Entry<String, GearStats> entry) -> {
            String[] parts = entry.getKey().split(":"); // Split "front:rear"
            int front = Integer.parseInt(parts[0]);
            int rear = Integer.parseInt(parts[1]);
            return front * 100 + rear; // Sorting priority: first by front, then rear
        }));
        
        // Now print the sorted output
        for (var entry : sortedGears) {
            String gear = entry.getKey();
            GearStats stats = entry.getValue();
            double avgSpeed = stats.totalSpeed.doubleValue() / stats.numRecords;
            double avgCadence = (double) stats.totalCadence / stats.numRecords;
            double avgPower = (double) stats.totalPower / stats.numRecords;
            String totalTime = timeConvert(stats.totalTimeSeconds);
        
            System.out.printf("⚙ Gear %s → ⏳ Time: %s sec, 🚴 Avg Speed: %.2f km/h, 🔄 Avg Cadence: %.1f, ⚡ Avg Power: %.1fW%n",
                    gear, totalTime, avgSpeed, avgCadence, avgPower);
        }
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
        rideRecords.forEach(ride -> 
            System.out.printf("⏱ Timestamp: %d, ⚙ Gear: %d:%d, 🚴 Speed: %.2f km/h, 🔄 Cadence: %d, ⚡ Power: %dW%n",
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
                        case "record" -> handleRecordMessage(mesg);
                        case "event" -> handleEventMessage(mesg);
                        case "session" -> System.out.println("📊 Session info received.");
                        default -> {} // Ignore other messages
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
        } else {
            System.out.println("Data is null");
        }
    }

    private static void handleEventMessage(Mesg mesg) {
        String eventType = "";
        Short eventValue = null;

        for (var field : mesg.getFields()) {
            if (field.getName().equals("event")) {
                eventValue = (Short) field.getValue();
                eventType = com.garmin.fit.Event.getByValue(eventValue).toString();
              //  System.out.println("Event detected" + eventType);
            }
        }

        switch (eventType) {
            case "REAR_GEAR_CHANGE" -> updateRearGear(mesg);
            case "FRONT_GEAR_CHANGE" -> updateFrontGear(mesg);
        }
    }

    private static void updateRearGear(Mesg mesg) {
        for(var field : mesg.getFields()) {
            if ("rear_gear_num".equals(field.getName())) {
                currentRearGear = (Short) field.getValue();
             // gear:  " + currentRearGear);
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

        // 🔄 debug
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

