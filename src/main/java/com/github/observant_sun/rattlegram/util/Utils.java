package com.github.observant_sun.rattlegram.util;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

@Slf4j
public final class Utils {

    private Utils() {}

    public static byte[] shortArrayToNewByteArray(short[] shortArray) {
        byte[] bytes = new byte[shortArray.length * 2];
        for (int i = 0; i < shortArray.length; i++) {
            bytes[2 * i] = (byte) shortArray[i];
            bytes[2 * i + 1] = (byte) (shortArray[i] >> 8);
        }
        return bytes;
    }

    public static Optional<Integer> stringToInteger(String string) {
        Integer integer = null;
        try {
            integer = Integer.parseInt(string);
        } catch (NumberFormatException ignored) {}
        return Optional.ofNullable(integer);
    }

}
