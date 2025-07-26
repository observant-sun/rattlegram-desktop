package com.github.observant_sun.rattlegram.prefs;

public enum AudioMode {
    MONO(1),
    STEREO(2),
    ;

    private final int channelCount;

    AudioMode(int channelCount) {
        this.channelCount = channelCount;
    }

    public int getChannelCount() {
        return channelCount;
    }
}
