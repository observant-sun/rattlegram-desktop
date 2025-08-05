package com.github.observant_sun.rattlegram.prefs;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

@Slf4j
class AppPreferencesImpl implements AppPreferences {

    private final Properties defaults;

    private AppPreferencesImpl() {
        defaults = new Properties();
        try (InputStream input = getClass().getResourceAsStream("default-preferences.properties")) {
            defaults.load(input);
        } catch (IOException ex) {
            log.error(ex.getMessage(), ex);
        }
        for (Pref pref : Pref.values()) {
            Object defaultValue = defaults.get(pref.name());
            if (defaultValue == null) {
                log.warn("Default value for {} is undefined", pref.name());
            }
        }
        synchronizeVersions();
    }

    private static final class InstanceHolder {
        private static final AppPreferences instance = new AppPreferencesImpl();
    }

    public static AppPreferences getInstance() {
        return InstanceHolder.instance;
    }

    private final Preferences prefs = Preferences.userNodeForPackage(AppPreferences.class);

    private void synchronizeVersions() {
        int savedVersion = prefs.getInt(Pref.PREFERENCES_VERSION.name(), -1);
        int actualVersion = Integer.parseInt(defaults.getProperty(Pref.PREFERENCES_VERSION.name()));
        if (savedVersion == -1 || savedVersion != actualVersion) {
            clear();
            prefs.putInt(Pref.PREFERENCES_VERSION.name(), actualVersion);
        }
    }

    @Override
    public void clear() {
        try {
            prefs.clear();
        } catch (BackingStoreException e) {
            log.error("Clear default preferences failed", e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public void set(Pref key, Object value) {
        log.debug("set setting {} to {}", key.name(), value);
        if (value == null) {
            throw new NullPointerException("value is null for key " + key.name());
        }
        if (!key.getPrefClass().isInstance(value)) {
            throw new IllegalArgumentException("Invalid value for key " + key.name() + ": " + value);
        }
        if (value instanceof Enum) {
            prefs.put(key.name(), ((Enum<?>) value).name());
            return;
        }
        prefs.put(key.name(), value.toString());

    }

    @Override
    public <T> T get(Pref key, Class<T> prefValueClass) {
        log.debug("get setting {} as {}", key.name(), prefValueClass.getName());
        if (!key.getPrefClass().equals(prefValueClass)) {
            throw new IllegalArgumentException("Invalid class for key " + key.name() + ": " + prefValueClass);
        }
        String value = prefs.get(key.name(), null);
        if (value == null) {
            value = defaults.getProperty(key.name());
        }
        if (value == null) {
            throw new IllegalArgumentException("Value for key " + key.name() + " is undefined");
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
