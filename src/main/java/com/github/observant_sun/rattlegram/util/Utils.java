package com.github.observant_sun.rattlegram.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Utils {

    private static final Logger log = LoggerFactory.getLogger(Utils.class);

    private Utils() {}

    public static byte[] shortArrayToNewByteArray(short[] shortArray) {
        byte[] bytes = new byte[shortArray.length * 2];
        for (int i = 0; i < shortArray.length; i++) {
            bytes[2 * i] = (byte) shortArray[i];
            bytes[2 * i + 1] = (byte) (shortArray[i] >> 8);
        }
        return bytes;
    }
}
