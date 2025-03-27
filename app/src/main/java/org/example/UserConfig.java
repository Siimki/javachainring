package org.example;

public class UserConfig {
    private final Integer minCadence;
    private final Integer minPower;
    private final String cassette;
    private final Integer bigChainring;
    private final Integer smallChainring;
    private final Boolean oneBySetup;

    public UserConfig(Integer minCadence, Integer minPower, String cassette, Integer bigChainring, Integer smallChainring, Boolean oneBySetup) {
        this.minCadence = minCadence;
        this.minPower = minPower;
        this.cassette = cassette;
        this.bigChainring = bigChainring;
        this.smallChainring = smallChainring;
        this.oneBySetup = oneBySetup; 
    }

    public Integer getMinCadence() { return minCadence; }
    public Integer getMinPower() { return minPower; }
    public String getCassette() { return cassette; }
    public Integer getBigChainring() { return bigChainring; }
    public Integer getSmallChainring() { return smallChainring; }
    public Boolean getOneBySetup() { return oneBySetup;}
}
