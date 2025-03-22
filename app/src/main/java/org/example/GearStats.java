package org.example;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

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

        public Map<String, Object> toJson() {
            Map<String, Object> json = new HashMap<>();
            json.put("totalSpeed", totalSpeed);
            json.put("totalCadence", totalCadence);
            json.put("totalPower", totalPower);
            json.put("totalTimeSeconds", totalTimeSeconds);
            json.put("numRecords", numRecords);
            return json;
        }
    
}