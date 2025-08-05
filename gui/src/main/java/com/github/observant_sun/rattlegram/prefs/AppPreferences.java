package com.github.observant_sun.rattlegram.prefs;

public interface AppPreferences {

    void clear();
    void set(Pref key, Object value);
    <T> T get(Pref key, Class<T> prefValueClass);

    static AppPreferences get() {
        return AppPreferencesImpl.getInstance();
    }
}
