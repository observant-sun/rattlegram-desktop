package com.github.observant_sun.rattlegram.prefs;

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

    public int getIntValue() {
        return intValue;
    }

    public int getChannelCount() {
        return channelCount;
    }
}
