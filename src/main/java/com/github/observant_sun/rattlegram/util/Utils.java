package com.github.observant_sun.rattlegram.util;

import lombok.extern.slf4j.Slf4j;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
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

    public static String truncateStringToUtf8ByteLength(String string, int maxByteLength) {
        Charset charset = StandardCharsets.UTF_8;
        byte[] bytes = string.getBytes(charset);
        if (bytes.length <= maxByteLength) {
            return string;
        }
        ByteBuffer byteBuffer = ByteBuffer.wrap(bytes, 0, maxByteLength);
        CharBuffer charBuffer = CharBuffer.allocate(maxByteLength);
        CharsetDecoder decoder = charset.newDecoder();
        decoder.onMalformedInput(CodingErrorAction.IGNORE);
        decoder.decode(byteBuffer, charBuffer, true);
        decoder.flush(charBuffer);
        string = new String(charBuffer.array(), 0, charBuffer.position());
        return string;
    }
}
