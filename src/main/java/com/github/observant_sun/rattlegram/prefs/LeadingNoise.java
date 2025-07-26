package com.github.observant_sun.rattlegram.prefs;

public enum LeadingNoise {
    DISABLED(0),
    QUARTER_SECOND(1),
    HALF_SECOND(3),
    ONE_SECOND(6),
    TWO_SECONDS(11),
    FOUR_SECONDS(22),
    ;

    private final int noiseSymbols;

    LeadingNoise(int noiseSymbols) {
        this.noiseSymbols = noiseSymbols;
    }

    public int getNoiseSymbols() {
        return noiseSymbols;
    }
}
