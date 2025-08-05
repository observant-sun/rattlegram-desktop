package com.github.observant_sun.rattlegram.i18n;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.ResourceBundle;

@Getter
@Slf4j
public class I18n {

    private static final class InstanceHolder {
        private static final I18n instance = new I18n();
    }

    public static I18n get() {
        return InstanceHolder.instance;
    }

    private final ResourceBundle resourceBundle;

    private I18n() {
        resourceBundle = ResourceBundle.getBundle("i18n_strings");
    }

    public String getMessage(String key) {
        return resourceBundle.getString(key);
    }

    public String getMessage(Enum<?> enumKey) {
        return getMessage(enumKey.getClass().getName() + "." + enumKey.name());
    }

    public String getMessage(Class<?> clazz, String key) {
        return getMessage(clazz.getName() + "." + key);
    }

}
