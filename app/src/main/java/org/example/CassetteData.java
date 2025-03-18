package org.example;

import java.util.HashMap;
import java.util.Map;

public class CassetteData {

    private static final Map<String, int[]> cassetteMap = new HashMap<>();

    static {
        // Shimano 12-speed cassettes
        cassetteMap.put("12shimano34", new int[]{34, 30, 27, 24, 21, 19, 17, 15, 14, 13, 12, 11});
        cassetteMap.put("12shimano30", new int[]{30, 27, 24, 21, 19, 17, 16, 15, 14, 13, 12, 11});
    
        // Shimano 11-speed cassettes
        cassetteMap.put("11shimano25", new int[]{25, 23, 21, 19, 17, 16, 15, 14, 13, 12, 11});
        cassetteMap.put("11shimano28", new int[]{28, 25, 23, 21, 19, 17, 15, 14, 13, 12, 11});
        cassetteMap.put("11shimano30", new int[]{30, 27, 24, 21, 19, 17, 15, 14, 13, 12, 11});
        cassetteMap.put("11shimano32", new int[]{32, 28, 25, 22, 20, 18, 16, 14, 13, 12, 11});
        cassetteMap.put("11shimano19", new int[]{25, 23, 21, 19, 18, 17, 16, 15, 14, 13, 12});
        cassetteMap.put("11shimano28wide", new int[]{28, 25, 23, 21, 20, 19, 18, 17, 16, 15, 14});
    
        // SRAM 12-speed cassettes
        cassetteMap.put("12sram28", new int[]{28, 24, 21, 19, 17, 16, 15, 14, 13, 12, 11, 10});
        cassetteMap.put("12sram30", new int[]{30, 27, 24, 21, 19, 17, 15, 14, 13, 12, 11, 10});
        cassetteMap.put("12sram33", new int[]{33, 28, 24, 21, 19, 17, 15, 14, 13, 12, 11, 10});
        cassetteMap.put("12sram36", new int[]{36, 32, 28, 24, 21, 19, 17, 15, 13, 12, 11, 10});
        cassetteMap.put("12sram44", new int[]{44, 38, 32, 28, 24, 21, 19, 17, 15, 13, 11, 10});
        cassetteMap.put("12sram46", new int[]{46, 38, 32, 28, 24, 21, 19, 17, 15, 13, 12, 10});
        cassetteMap.put("12sram50", new int[]{50, 42, 36, 32, 28, 24, 21, 18, 16, 14, 12, 10});
        cassetteMap.put("12sram52", new int[]{52, 44, 38, 32, 28, 24, 21, 18, 16, 14, 12, 10});

         // SRAM 13-speed cassette
         cassetteMap.put("13sram46", new int[]{10, 11, 12, 13, 15, 17, 19, 21, 24, 28, 32, 38, 46});
    }
    

    public static int[] getCassette(String cassette) {
        return cassetteMap.getOrDefault(cassette, new int[]{});
    }

}
