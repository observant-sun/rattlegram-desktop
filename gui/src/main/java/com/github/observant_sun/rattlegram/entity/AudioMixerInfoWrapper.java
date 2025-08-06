package com.github.observant_sun.rattlegram.entity;

import javax.sound.sampled.Mixer;

public record AudioMixerInfoWrapper(
        String stringRepresentation,
        Mixer.Info mixerInfo
) {
    @Override
    public String toString() {
        return stringRepresentation;
    }
}
