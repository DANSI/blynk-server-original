package cc.blynk.utils;

import cc.blynk.server.core.protocol.enums.Response;
import cc.blynk.server.core.protocol.exceptions.BaseServerException;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 1/31/2015.
 */
public final class ParseUtil {

    public static int parseInt(String intProperty) {
        try {
            return Integer.parseInt(intProperty);
        } catch (NumberFormatException nfe) {
            throw new RuntimeException(intProperty + " not a number. " + nfe.getMessage());
        }
    }

    public static int parseInt(String intProperty, int msgId) {
        try {
            return Integer.parseInt(intProperty);
        } catch (NumberFormatException ex) {
            throw new BaseServerException(String.format("Id '%s' not valid.", intProperty), msgId, Response.ILLEGAL_COMMAND);
        }
    }

    public static byte parseByte(String intProperty, int msgId) {
        try {
            return Byte.parseByte(intProperty);
        } catch (NumberFormatException ex) {
            throw new BaseServerException("Pin is not a number.", msgId, Response.ILLEGAL_COMMAND);
        }
    }

    public static long parseLong(String longProperty) {
        try {
            return Long.parseLong(longProperty);
        } catch (NumberFormatException nfe) {
            throw new RuntimeException(longProperty + " not a number. " + nfe.getMessage());
        }
    }

    public static int parseLong(String longProperty, int msgId) {
        try {
            return Integer.parseInt(longProperty);
        } catch (NumberFormatException ex) {
            throw new BaseServerException(String.format("Id '%s' not valid.", longProperty), msgId, Response.ILLEGAL_COMMAND);
        }
    }

}
