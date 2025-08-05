package com.github.observant_sun.rattlegram.prefs;

public enum SampleRate {
    SAMPLE_RATE_8000(8000),
    SAMPLE_RATE_16000(16000),
    SAMPLE_RATE_32000(32000),
    SAMPLE_RATE_44100(44100),
    SAMPLE_RATE_48000(48000),
    ;

    final int rateValue;

    SampleRate(int rateValue) {
        this.rateValue = rateValue;
    }

    public int getRateValue() {
        return rateValue;
    }

    @Override
    public String toString() {
        return rateValue + "";
    }
}
