package com.github.observant_sun.rattlegram.prefs;

public enum InputChannel {
    DEFAULT(0),
    FIRST(1),
    SECOND(2),
    SUMMATION(3),
    ANALYTIC(4),
    ;

    private final int intValue;

    InputChannel(int intValue) {
        this.intValue = intValue;
    }

    public int getIntValue() {
        return intValue;
    }
}
