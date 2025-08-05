package com.github.observant_sun.rattlegram.prefs;

import com.github.observant_sun.rattlegram.i18n.I18n;
import lombok.Getter;

@Getter
public enum OutputChannel {
    DEFAULT(0, 1),
    FIRST(1, 2),
    SECOND(2, 2),
    ANALYTIC(4, 2),
    ;

    private final int intValue;
    private final int channelCount;

    OutputChannel(int intValue, int channelCount) {
        this.intValue = intValue;
        this.channelCount = channelCount;
    }

    @Override
    public String toString() {
        return I18n.get().getMessage(this);
    }
}
