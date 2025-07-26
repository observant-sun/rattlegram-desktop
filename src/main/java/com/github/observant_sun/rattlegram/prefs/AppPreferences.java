package com.github.observant_sun.rattlegram.prefs;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.prefs.Preferences;

public class AppPreferences {

    private static final Logger log = LoggerFactory.getLogger(AppPreferences.class);

    private final Properties defaults;

    public enum Pref {
        INPUT_SAMPLE_RATE(SampleRate.class),
        CARRIER_FREQUENCY(Integer.class),
        LEADING_NOISE(LeadingNoise.class),
        FANCY_HEADER(Boolean.class),
        INPUT_CHANNEL(InputChannel.class),
        INPUT_AUDIO_MODE(AudioMode.class),
        OUTPUT_SAMPLE_RATE(SampleRate.class),
        OUTPUT_CHANNEL(OutputChannel.class),
        OUTPUT_AUDIO_MODE(AudioMode.class),
        CALLSIGN(String.class),
        ;

        private final Class<?> prefClass;

        Pref(Class<?> prefClass) {
            this.prefClass = prefClass;
        }

        public Class<?> getPrefClass() {
            return prefClass;
        }

    }


    private AppPreferences() {
        defaults = new Properties();
        try (InputStream input = getClass().getResourceAsStream("default-preferences.properties")) {
            defaults.load(input);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        for (Pref pref : Pref.values()) {
            Object defaultValue = defaults.get(pref.toString());
            if (defaultValue == null) {
                log.warn("Default value for {} is undefined", pref);
            }
        }
    }

    private static final class InstanceHolder {
        private static final AppPreferences instance = new AppPreferences();
    }

    public static AppPreferences get() {
        return InstanceHolder.instance;
    }

    public void set(Pref key, Object value) {
        log.debug("set setting {} to {}", key, value);
        Preferences prefs = Preferences.userRoot();
        if (value == null) {
            throw new NullPointerException("value is null for key " + key);
        }
        if (!key.getPrefClass().isInstance(value)) {
            throw new IllegalArgumentException("Invalid value for key " + key + ": " + value);
        }
        if (value instanceof Enum) {
            prefs.put(key.toString(), ((Enum<?>) value).name());
            return;
        }
        prefs.put(key.toString(), value.toString());

    }

    public <T> T get(Pref key, Class<T> prefValueClass) {
        log.debug("get setting {} as {}", key, prefValueClass.getName());
        if (!key.getPrefClass().equals(prefValueClass)) {
            throw new IllegalArgumentException("Invalid class for key " + key + ": " + prefValueClass);
        }
        Preferences prefs = Preferences.userRoot();
        String value = prefs.get(key.toString(), null);
        if (value == null) {
            value = defaults.getProperty(key.toString());
        }
        if (value == null) {
            throw new IllegalArgumentException("Value for key " + key + " is undefined");
        }
        if (Enum.class.isAssignableFrom(prefValueClass)) {
            @SuppressWarnings("unchecked")
            Enum<?> valueAsEnum = Enum.valueOf((Class<? extends Enum>) prefValueClass, value);
            return prefValueClass.cast(valueAsEnum);
        }
        if (Integer.class.isAssignableFrom(prefValueClass)) {
            return prefValueClass.cast(Integer.valueOf(value));
        }
        if (Long.class.isAssignableFrom(prefValueClass)) {
            return prefValueClass.cast(Long.valueOf(value));
        }
        if (Float.class.isAssignableFrom(prefValueClass)) {
            return prefValueClass.cast(Float.valueOf(value));
        }
        if (Double.class.isAssignableFrom(prefValueClass)) {
            return prefValueClass.cast(Double.valueOf(value));
        }
        if (Boolean.class.isAssignableFrom(prefValueClass)) {
            return prefValueClass.cast(Boolean.valueOf(value));
        }
        if (String.class.isAssignableFrom(prefValueClass)) {
            return prefValueClass.cast(value);
        }
        throw new IllegalArgumentException("Unsupported pref value class: " + prefValueClass);
    }

}
