package org.example;

public class UserConfig {
    private final Integer minCadence;
    private final Integer minPower;
    private final String cassette;
    private final Integer bigChainring;
    private final Integer smallChainring;

    public UserConfig(Integer minCadence, Integer minPower, String cassette, Integer bigChainring, Integer smallChainring) {
        this.minCadence = minCadence;
        this.minPower = minPower;
        this.cassette = cassette;
        this.bigChainring = bigChainring;
        this.smallChainring = smallChainring;
    }

    public Integer getMinCadence() { return minCadence; }
    public Integer getMinPower() { return minPower; }
    public String getCassette() { return cassette; }
    public Integer getBigChainring() { return bigChainring; }
    public Integer getSmallChainring() { return smallChainring; }
}
