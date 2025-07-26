package com.github.observant_sun.rattlegram.entity;

public record TransmissionSettings(
        int carrierFrequency,
        int noiseSymbols,
        boolean fancyHeader,
        int channelSelect
) {
}
