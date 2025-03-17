package org.example;

import java.math.BigDecimal;

public class RideSummary {
    private final long timestamp;
    private final BigDecimal totalDistance;
    private final Long totalWork;
    private final Long totalMovingTime;
    private final Integer totalCalories;
    private final BigDecimal avgSpeed;
    private final BigDecimal maxSpeed;
    private final Integer avgPower;
    private final Integer maxPower;
    private final Integer totalAscent;
    private final Integer avgHeartRate;
    private final Integer maxHeartRate;
    private final Integer avgCadence;
    private final Integer maxCadence;
    private final Integer avgTemperature;
    private final Integer maxTemperature;
    private final BigDecimal enhancedAvgSpeed;

    private RideSummary(Builder builder) {
        this.timestamp = builder.timestamp;
        this.totalDistance = builder.totalDistance;
        this.totalWork = builder.totalWork;
        this.totalMovingTime = builder.totalMovingTime;
        this.totalCalories = builder.totalCalories;
        this.avgSpeed = builder.avgSpeed;
        this.maxSpeed = builder.maxSpeed;
        this.avgPower = builder.avgPower;
        this.maxPower = builder.maxPower;
        this.totalAscent = builder.totalAscent;
        this.avgHeartRate = builder.avgHeartRate;
        this.maxHeartRate = builder.maxHeartRate;
        this.avgCadence = builder.avgCadence;
        this.maxCadence = builder.maxCadence;
        this.avgTemperature = builder.avgTemperature;
        this.maxTemperature = builder.maxTemperature;
        this.enhancedAvgSpeed = builder.enhancedAvgSpeed;
    }

    public static class Builder {
        private final long timestamp;
        private final BigDecimal totalDistance;
        private Long totalWork;
        private Long totalMovingTime;
        private Integer totalCalories;
        private BigDecimal avgSpeed;
        private BigDecimal maxSpeed;
        private Integer avgPower;
        private Integer maxPower;
        private Integer totalAscent;
        private Integer avgHeartRate;
        private Integer maxHeartRate;
        private Integer avgCadence;
        private Integer maxCadence;
        private Integer avgTemperature;
        private Integer maxTemperature;
        private BigDecimal enhancedAvgSpeed;

        public Builder(long timestamp, BigDecimal totalDistance) {
            this.timestamp = timestamp;
            this.totalDistance = totalDistance;
        }

        public Builder totalWork(Long totalWork) { this.totalWork = totalWork; return this; }
        public Builder totalMovingTime(Long totalMovingTime) { this.totalMovingTime = totalMovingTime; return this; }
        public Builder totalCalories(Integer totalCalories) { this.totalCalories = totalCalories; return this; }
        public Builder avgSpeed(BigDecimal avgSpeed) { this.avgSpeed = avgSpeed; return this; }
        public Builder maxSpeed(BigDecimal maxSpeed) { this.maxSpeed = maxSpeed; return this; }
        public Builder avgPower(Integer avgPower) { this.avgPower = avgPower; return this; }
        public Builder maxPower(Integer maxPower) { this.maxPower = maxPower; return this; }
        public Builder totalAscent(Integer totalAscent) { this.totalAscent = totalAscent; return this; }
        public Builder avgHeartRate(Integer avgHeartRate) { this.avgHeartRate = avgHeartRate; return this; }
        public Builder maxHeartRate(Integer maxHeartRate) { this.maxHeartRate = maxHeartRate; return this; }
        public Builder avgCadence(Integer avgCadence) { this.avgCadence = avgCadence; return this; }
        public Builder maxCadence(Integer maxCadence) { this.maxCadence = maxCadence; return this; }
        public Builder avgTemperature(Integer avgTemperature) { this.avgTemperature = avgTemperature; return this; }
        public Builder maxTemperature(Integer maxTemperature) { this.maxTemperature = maxTemperature; return this; }
        public Builder enhancedAvgSpeed(BigDecimal enhancedAvgSpeed) { this.enhancedAvgSpeed = enhancedAvgSpeed; return this; }

        public RideSummary build() {
            return new RideSummary(this);
        }
    }
    @Override
    public String toString() {
        return """
               \n --- RideSummary --- \n
                timestamp = """ + timestamp +
                "\n totalDistance = " + convertMetersToKilometers(totalDistance) + " km" +
                "\n totalCalories = " + totalCalories + " kcal" +
                "\n avgPower = " + avgPower + "W" +
                "\n maxPower = " + maxPower +  "W" +
                "\n totalAscent = " + totalAscent + " m" +
                "\n avgSpeed = " + convertMetersPerSecondToKmPerHour(avgSpeed) + " km/h" +
                "\n maxSpeed = " + convertMetersPerSecondToKmPerHour(maxSpeed) + " km/h" +
                '\n';
    }
    
    private BigDecimal convertMetersToKilometers(BigDecimal meters) {
        return meters != null ? meters.divide(BigDecimal.valueOf(1000),2, BigDecimal.ROUND_HALF_UP) : BigDecimal.valueOf(0.00);
    }

    private BigDecimal convertMetersPerSecondToKmPerHour(BigDecimal mps) {
        return mps != null ? mps.multiply(BigDecimal.valueOf(3.6)).setScale(2, BigDecimal.ROUND_HALF_UP) : BigDecimal.valueOf(0.00);
    }
}

// Message received: event
//  -  timestamp: 1109832070
//  -  event: 43
//  -  event_type: 3
//  -  front_gear_num: 2
//  -  rear_gear_num: 12