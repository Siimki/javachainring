package org.example;

import java.math.BigDecimal;

public class RideData {
    private final long timestamp;
    private final int frontGear;
    private final int rearGear;
    private final BigDecimal speed;
    private final int cadence;
    private final int power;

    public RideData(long timestamp, int frontGear, int rearGear, BigDecimal speed, int cadence, int power) {
        this.timestamp = timestamp;
        this.frontGear = frontGear;
        this.rearGear = rearGear;
        this.speed = speed.multiply(BigDecimal.valueOf(3.6)); //calculation from m/s to km/h
        this.cadence = cadence;
        this.power = power;
    }

    public long getTimeStamp() { return timestamp; }
    public int getFrontGear() { return frontGear; }
    public int getRearGear() { return rearGear; }
    public BigDecimal getSpeed() { return speed; }
    public int getCadence() { return cadence; }
    public int getPower() { return power; }

}


