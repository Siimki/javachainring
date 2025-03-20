package org.example;

import java.math.BigDecimal;

public class GearStats {
    BigDecimal totalSpeed = BigDecimal.ZERO;
    int totalCadence = 0;
    int totalPower = 0;
    int totalTimeSeconds = 0;
    int numRecords = 0;

    public void addRideData(RideData ride) {
        this.totalSpeed = this.totalSpeed.add(ride.getSpeed());
        this.totalCadence += ride.getCadence();
        this.totalPower += ride.getPower();
        this.totalTimeSeconds++;
        this.numRecords++;
    }

    public BigDecimal getTotalSpeed() {
        return totalSpeed;
    }

    public int getTotalCadence() {
        return totalCadence;
    }

    public int getTotalPower() {
        return totalPower;
    }

    public int getTotalTimeSeconds() {
        return totalTimeSeconds;
    }

    public int getNumRecords() {
        return numRecords;
    }
}