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

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

import com.garmin.fit.Decode;
import com.garmin.fit.Mesg;
import com.garmin.fit.MesgBroadcaster;
import com.garmin.fit.MesgListener;


@SpringBootApplication
@ComponentScan("org.example.controllers")
public class App {

    public static Map<String, Object> analyzeFile(File fitFile, UserConfig userConfig) {
        RideSession session = new RideSession();
        try {
            long maxFileSize = 15 * 1024 * 1024; // 15MB in bytes
            if (fitFile.length() > maxFileSize) {
                return Map.of("error", "File is too large. Maximum allowed size is 10MB.");
            }        
            // // reset gears as well. 
            // currentRearGear = 0;
            // currentFrontGear = 2; 
            // List<RideData> rideRecords = getRideRecords();
            
            processFitFile(fitFile, session);

            
            if (session.rideRecords.isEmpty()) {
                return Map.of("error", "No ride data found in the FIT file.");
            }
            // Estimator 
            // Estimator estimator = new Estimator();
            // String suggestedChainring = estimator.estimateOptimalSingleChainring(session, userConfig);
            // System.out.println("🔧 Suggested Single Chainring: " + suggestedChainring);
            Map<String, GearStats> gearStatsMap = analyzeGearUsage(session, userConfig);
            return getGearUsageAsJson(gearStatsMap, userConfig);

        } catch (Exception e) {
            e.printStackTrace();
            return Map.of("error", "Failed to process the FIT file");
        } finally {
            session.rideRecords.clear();
        }
    }

    private static Map<String, GearStats> analyzeGearUsage(RideSession session, UserConfig config) {
        Map<String, GearStats> gearStatsMap = new HashMap<>();

        for (RideData ride : session.rideRecords) {
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

    private static Map<String, Object> getGearUsageAsJson(Map<String, GearStats> rawGearStatsMap, UserConfig config) {
        List<Map.Entry<String, GearStats>> gearStatsMap = new ArrayList<>(rawGearStatsMap.entrySet());
        gearStatsMap.sort(Comparator.comparingInt(entry
            -> parseGear(entry.getKey())
        ));

      
        List<Map<String, Object>> formattedGears = new ArrayList<>();
        
        int totalRideTime = gearStatsMap.stream()
            .mapToInt(entry -> entry.getValue().getTotalTimeSeconds()).sum();
    
        int totalRedTime = 0, totalOrangeTime = 0, totalGreenTime = 0;
    
        int[] cassetteTeeth = CassetteData.getCassette(config.getCassette());
        if (cassetteTeeth == null) cassetteTeeth = new int[0];
        Integer lostSecondsCounter = 0;
        Boolean oneBySetup = config.getOneBySetup();

        for (Map.Entry<String, GearStats> entry : gearStatsMap) {
            String gear = entry.getKey();
            GearStats stats = entry.getValue();
            int[] gearNumbers = extractGearNumbers(gear);
            int frontIndex = gearNumbers[0] - 1;
            int rearIndex = gearNumbers[1] - 1;
            int frontChainring = (frontIndex == 0) ? config.getSmallChainring() : config.getBigChainring();
            int rearTeeth = (rearIndex >= 0 && rearIndex < cassetteTeeth.length) ? cassetteTeeth[rearIndex] : 0;
            double gearRatio = (rearTeeth > 0) ? (double) frontChainring / rearTeeth : 0;
            
            // ✅ Format stats into JSON
            Map<String, Object> gearJson = formatGearStats(gear, frontChainring, rearTeeth, gearRatio, stats, totalRideTime);
            formattedGears.add(gearJson);
    
            // ✅ Calculate time spent in each zone
            String zone = classifyGearZone(rearTeeth, cassetteTeeth, oneBySetup, frontIndex);
            switch (zone) {
                case "🔴 Red Zone" -> totalRedTime += stats.getTotalTimeSeconds();
                case "🟠 Orange Zone" -> totalOrangeTime += stats.getTotalTimeSeconds();
                case "🟢 Green Zone" -> totalGreenTime += stats.getTotalTimeSeconds();
                default -> lostSecondsCounter++;
            }
        }

        if (lostSecondsCounter > 60) {
            System.out.println("Check why so many seconds are lost!");
        }  else if (lostSecondsCounter > 600) {
            System.out.println("Check why we fcked up!");
            System.out.println("Lost seconds counter" + lostSecondsCounter);
        }
        // ✅ Add the zone summary to the API response
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "File processed successfully");
        response.put("gears_used", gearStatsMap.size());
       // System.out.println(formattedGears);
        response.put("gear_analysis", formattedGears);
        response.put("zone_summary", formatZoneSummary(totalRedTime, totalOrangeTime, totalGreenTime, totalRideTime));
        return response;
    }
    

    private static String  classifyGearZone(int rearTeeth, int[] cassetteTeeth, boolean oneBySetup, int frontChainring) {
        int len = cassetteTeeth.length;
       // System.out.println("Current frontRing" + frontChainring + oneBySetup + rearTeeth);
        if (oneBySetup) {
            if (rearTeeth == cassetteTeeth[0] || rearTeeth == cassetteTeeth[1] || rearTeeth == cassetteTeeth[len - 1] ) {
           //     System.out.println("Oneby red");
                return "🔴 Red Zone";
            } else if (rearTeeth == cassetteTeeth[2] || rearTeeth == cassetteTeeth[len - 3] || rearTeeth == cassetteTeeth[len - 2]) {
            //    System.out.println("Oneby orange");
                return "🟠 Orange Zone";
            }
          //  System.out.println("Oneby green");
            return "🟢 Green Zone";
        } else {

            boolean isBigRing = frontChainring == 1;
           // System.out.println(isBigRing + " NR Of chainring" + frontChainring + rearTeeth);
            if (isBigRing) {   
                if (rearTeeth == cassetteTeeth[0] ||  rearTeeth == cassetteTeeth[len - 1] || rearTeeth == cassetteTeeth[len -2] ) {
                //    System.out.println("twoBy red" + frontChainring + "and rear" + rearTeeth);
                    return "🔴 Red Zone";
                } else if ( rearTeeth == cassetteTeeth[1]  || rearTeeth == cassetteTeeth[len - 3]) {
                    // System.out.println("twoBy orange" + frontChainring + " and rear" + rearTeeth );
                    return "🟠 Orange Zone";
                }
              //  System.out.println("twoBy green" + frontChainring + rearTeeth);
                return "🟢 Green Zone";
            } else {
               // System.out.println(isBigRing + " NR Of chainring" + frontChainring + rearTeeth);
               // System.out.println("Do i come here more times? ");
                if (rearTeeth == cassetteTeeth[len - 4] || rearTeeth == cassetteTeeth[len - 3] || rearTeeth == cassetteTeeth[len - 2] || rearTeeth == cassetteTeeth[len - 1]) {
                    // System.out.println("twoBy red small" + frontChainring + " and rear " + rearTeeth);
                    return "🔴 Red Zone";
                } else if ( rearTeeth == cassetteTeeth[len - 5] || rearTeeth == cassetteTeeth[0]) {
                    // System.out.println("twoBy orange small" + frontChainring + " and rear" + rearTeeth);
                    return "🟠 Orange Zone";
                }
               // System.out.println("Two by green smaal" + rearTeeth);
                return "🟢 Green Zone";
            }
        }
    }


    private static Map<String, Object> formatGearStats( String gear, int frontTeeth, int rearTeeth, double gearRatio, GearStats stats, int totalRideTime) {
    double usagePercentage = (stats.totalTimeSeconds * 100.0) / totalRideTime;
    
    // ✅ Create formatted string (for logs / debugging)
    String formattedOutput = String.format(
        "⚙ Gear %s (%dT:%dT | Ratio: %.2f) → ⏳ Time: %s sec (%.1f%%), 🚴 Avg Speed: %.2f km/h,  Avg Cadence: %.1f, ⚡ Avg Power: %.1fW",
        gear, frontTeeth, rearTeeth, gearRatio, timeConvert(stats.totalTimeSeconds), usagePercentage,
        stats.totalSpeed.doubleValue() / stats.numRecords, 
        (double) stats.totalCadence / stats.numRecords, 
        (double) stats.totalPower / stats.numRecords
    );

    // ✅ Create JSON output (for API)
    Map<String, Object> gearJson = new HashMap<>();
    gearJson.put("gear", gear);
    gearJson.put("front_teeth", frontTeeth);
    gearJson.put("rear_teeth", rearTeeth);
    gearJson.put("gear_ratio", String.format("%.2f", gearRatio));
    gearJson.put("total_time", timeConvert(stats.totalTimeSeconds));
    gearJson.put("usage_percentage", String.format("%.1f%%", usagePercentage));
    gearJson.put("avg_speed", String.format("%.2f km/h", stats.totalSpeed.doubleValue() / stats.numRecords));
    gearJson.put("avg_cadence", String.format("%.1f", (double) stats.totalCadence / stats.numRecords));
    gearJson.put("avg_power", String.format("%.1f W", (double) stats.totalPower / stats.numRecords));
    gearJson.put("formatted_output", formattedOutput); // ✅ Include formatted string in JSON

    return gearJson;
    }
    // keep it for debug 
    // private static void printZoneSummary(int totalRedTime, int totalOrangeTime, int totalGreenTime, int totalRideTime) {
    //     System.out.println("\n📊 Zone Summary:");
    //     System.out.printf("🔴 Red Zone: %s (%.1f%%)%n", timeConvert(totalRedTime), (totalRedTime * 100.0) / totalRideTime);
    //     System.out.printf("🟠 Orange Zone: %s (%.1f%%)%n", timeConvert(totalOrangeTime), (totalOrangeTime * 100.0) / totalRideTime);
    //     System.out.printf("🟢 Green Zone: %s (%.1f%%)%n", timeConvert(totalGreenTime), (totalGreenTime * 100.0) / totalRideTime);
    // }

    private static Map<String, Object> formatZoneSummary(int totalRedTime, int totalOrangeTime, int totalGreenTime, int totalRideTime) {
    Map<String, Object> zoneSummary = new HashMap<>();

    zoneSummary.put("red_zone", Map.of(
        "time", timeConvert(totalRedTime),
        "percentage", String.format("%.1f%%", (totalRedTime * 100.0) / totalRideTime)
    ));

    zoneSummary.put("orange_zone", Map.of(
        "time", timeConvert(totalOrangeTime),
        "percentage", String.format("%.1f%%", (totalOrangeTime * 100.0) / totalRideTime)
    ));

    zoneSummary.put("green_zone", Map.of(
        "time", timeConvert(totalGreenTime),
        "percentage", String.format("%.1f%%", (totalGreenTime * 100.0) / totalRideTime)
    ));
    
    
    zoneSummary.put("total", Map.of(
        "time", timeConvert(totalRideTime)
    ));

    return zoneSummary;
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

    public static void processFitFile(File fitFile, RideSession session) {
        try (InputStream fitStream = new FileInputStream(fitFile)) {
            Decode decode = new Decode();
            MesgBroadcaster mesgBroadcaster = new MesgBroadcaster(decode);

            // Add listener for processing messages
            mesgBroadcaster.addListener(new MesgListener() {
                @Override
                public void onMesg(Mesg mesg) {
                     // System.out.println("📩 Message received: " + mesg.getName() );
                     // Log developer field definitions
                    //  System.out.println("📩 Message: " + mesg.getName());
                    //  for (var field : mesg.getFields()) {
                    //      String name = field.getName();
                    //      Object value = field.getValue();
                    //      System.out.println("   🔧 Field: " + name + " = " + value);
                    //  }
                    switch (mesg.getName()) {
                        case "record" ->
                            handleRecordMessage(mesg, session);
                        case "event" ->
                            handleEventMessage(mesg, session);
                        case "session" -> {
                            System.out.println("Session info received.");
                            // for (var field : mesg.getFields()) {
                            //     Object rawValue = field.getValue();
                            //     if (rawValue instanceof Number) {
                            //         short eventValue = ((Number) rawValue).shortValue();
                            //         String eventType = com.garmin.fit.Event.getByValue(eventValue).toString();
                            //         System.out.println("Session Event detected " + eventType);
                            //         System.out.println("Session Field Name: " + field.getName() + " → Value: " + rawValue + " Tipe " + eventType);
                            //     } else {
                            //         System.out.println("Session Field Name: " + field.getName() + " → Value: " + rawValue + " (not a numeric value)");
                            //     }
                            // }
                        }
                        default -> {
                           // System.out.println("Other info");
                            // for (var field : mesg.getFields()) {
                            //     Object rawValue = field.getValue();
                            //     if (rawValue instanceof Number) {
                            //         short eventValue = ((Number) rawValue).shortValue();
                            //         String eventType = com.garmin.fit.Event.getByValue(eventValue).toString();
                            //         System.out.println("Other info Event detected " + eventType);
                            //         System.out.println("Other info Field Name: " + field.getName() + " → Value: " + rawValue + " Tipe " + eventType);
                            //     } else {
                            //         System.out.println("Other info Field Name: " + field.getName() + " → Value: " + rawValue + " (not a numeric value)");
                            //     }
                            // }
                        }
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
    


    private static final int MAX_RECORDS = 1000000; 
    private static void handleRecordMessage(Mesg mesg, RideSession session) {
        RideData data = parseRecord(mesg, session);
        // for (var field : mesg.getFields()) {
        //     // Object value = field.getValue();
        //     // int fieldNum = field.getNum(); // The actual field index (e.g. 30)
        //     // System.out.println("Field #" + fieldNum + " = " + value);
        //     // if (fieldNum == 30 && value instanceof Number) {
        //     //     int rawGearState = ((Number) value).intValue();
            
        //     //     int frontIndex = rawGearState / 100;
        //     //     int rearIndex = rawGearState % 100;
            
        //     //     System.out.println("🦷 Detected: Front Gear #" + frontIndex + " | Rear Gear #" + rearIndex);
        //     // }
            
        //    // System.out.println("Field Name in record: " + field.getName() + " → Value: " + value);
        // //   Long eventValue = (Long) field.getValue();
        // //   Short eventValue2 = eventValue != null ? (short) eventValue.longValue() : null;
        // //   String eventType = com.garmin.fit.Event.getByValue(eventValue2).toString();
        //   //System.out.println("Event detected" + eventType);

        //     //  System.out.println("Field Name: " + field.getName() + " → Value: " + field.getValue() + "Tipe " );
        // }
        if (data != null) {
            if (session.rideRecords.size() > MAX_RECORDS) {
                session.rideRecords.remove(0); //Remove oldest entry if too many
            }
            session.rideRecords.add(data);
        }
    }

    private static void printMessage(Mesg mesg) {
        for (var field : mesg.getFields()) {
            Object value = field.getValue();
            String eventType = (value instanceof Number)
                    ? com.garmin.fit.Event.getByValue(((Number) value).shortValue()).toString()
                    : "Unknown";
            // System.out.println("Message eventType is : " + eventType);
            // System.out.println("Field Name: " + field.getName() + " → Value: " + value);
            if ("REAR_GEAR_CHANGE".equals(eventType)) {
                //System.out.println("Gear change found" + " Value is: " + value);
                
            }
        }
    }

    private static void handleEventMessage(Mesg mesg, RideSession session) {
    
        for (var field : mesg.getFields()) {
            if (field.getName().equals("event")) {
                Short eventValue = (Short) field.getValue();
                String eventType = com.garmin.fit.Event.getByValue(eventValue).toString();
              //  System.out.println("Event detected" + eventType);

              //    System.out.println("Field Name: " + field.getName() + " → Value: " + field.getValue() + "Tipe " + eventType);
                if ("REAR_GEAR_CHANGE".equals(eventType)) {
                    //System.err.println("handleEventMessage: found rear gear" );
                    updateRearGear(mesg, session);
                } else if ("FRONT_GEAR_CHANGE".equals(eventType)) {
                    updateFrontGear(mesg, session);
                }
            }
        }
    }

    private static void updateRearGear(Mesg mesg, RideSession session) {
        for (var field : mesg.getFields()) {
            if ("rear_gear_num".equals(field.getName())) {
                session.currentRearGear = (Short) field.getValue();
            }
        }
    }

    private static void updateFrontGear(Mesg mesg, RideSession session) {
      //  System.out.println("🚴 Front gear change detected!");
        for (var field : mesg.getFields()) {
            if ("front_gear_num".equals(field.getName())) {
                session.currentFrontGear = (Short) field.getValue();
               // System.out.println("🔄 New Front Gear: " + currentFrontGear);
            }
        }
    }

    public static String getGreeting() {
        return "Hello, FIT World!";
    }

    private static RideData parseRecord(Mesg mesg, RideSession session) {
        long timestamp = getFieldLong(mesg, "timestamp", (int) 0L);
        //Lets see how this behaves. I changed it from speed to enhanced_speed. Some files had no speed field.  
        BigDecimal speed = getFieldBigDecimal(mesg, "enhanced_speed", BigDecimal.ZERO);
        int cadence = getFieldInt(mesg, "cadence", 0);
        int power = getFieldInt(mesg, "power", 0);

        // debug
        // mesg.getFields().forEach(field -> {
        //     System.out.println("Field Name: " + field.getName() + " → Value: " + field.getValue());
        // });
        return new RideData(timestamp, session.currentFrontGear, session.currentRearGear, speed, cadence, power);
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
    // public static List<RideData> getRideRecords() {
    //     return rideRecords;
    // }

}

