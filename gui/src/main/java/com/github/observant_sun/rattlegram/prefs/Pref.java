package com.github.observant_sun.rattlegram.prefs;

import com.github.observant_sun.rattlegram.i18n.I18n;
import lombok.Getter;

@Getter
public enum Pref {
    // increment PREFERENCES_VERSION in default-preferences.properties if there are incompatible changes
    PREFERENCES_VERSION(Integer.class),
    INPUT_SAMPLE_RATE(SampleRate.class),
    CARRIER_FREQUENCY(Integer.class),
    LEADING_NOISE(LeadingNoise.class),
    FANCY_HEADER(Boolean.class),
    INPUT_CHANNEL(InputChannel.class),
    OUTPUT_SAMPLE_RATE(SampleRate.class),
    OUTPUT_CHANNEL(OutputChannel.class),
    CALLSIGN(String.class),
    SHOW_SPECTRUM_ANALYZER(Boolean.class),
    STOP_LISTENING_WHEN_TRANSMITTING(Boolean.class),
    REPEATER_MODE_ENABLED(Boolean.class),
    REPEATER_DELAY(Integer.class),
    REPEATER_DEBOUNCE_TIME(Integer.class),
    BLOCK_OUTPUT_DRAIN_WORKAROUND(Boolean.class),
    ;

    private final Class<?> prefClass;

    Pref(Class<?> prefClass) {
        this.prefClass = prefClass;
    }

    @Override
    public String toString() {
        return I18n.get().getMessage(this);
    }
}
