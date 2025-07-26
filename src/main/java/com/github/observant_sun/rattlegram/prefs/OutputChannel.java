package com.github.observant_sun.rattlegram.prefs;

public enum OutputChannel {
    DEFAULT(0),
    FIRST(1),
    SECOND(2),
    ANALYTIC(4),
    ;

    private final int intValue;

    OutputChannel(int intValue) {
        this.intValue = intValue;
    }

    public int getIntValue() {
        return intValue;
    }
}
