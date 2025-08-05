package com.github.observant_sun.rattlegram.prefs;

import com.github.observant_sun.rattlegram.i18n.I18n;
import lombok.Getter;

@Getter
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

    @Override
    public String toString() {
        return I18n.get().getMessage(this);
    }
}
