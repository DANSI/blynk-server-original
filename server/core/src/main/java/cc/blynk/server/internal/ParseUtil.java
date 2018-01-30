package cc.blynk.server.internal;

import cc.blynk.server.core.protocol.exceptions.IllegalCommandException;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 1/31/2015.
 */
public final class ParseUtil {

    private ParseUtil() {
    }

    public static int parseInt(String intString) {
        try {
            return Integer.parseInt(intString);
        } catch (NumberFormatException ex) {
            throw new IllegalCommandException(intString + " not a valid int number.");
        }
    }

    public static byte parseByte(String byteString) {
        try {
            return Byte.parseByte(byteString);
        } catch (NumberFormatException ex) {
            throw new IllegalCommandException(byteString + " not a valid byte number.");
        }
    }

    public static long parseLong(String longString) {
        try {
            return Long.parseLong(longString);
        } catch (NumberFormatException nfe) {
            throw new IllegalCommandException(longString + " not a valid long number.");
        }
    }

}
