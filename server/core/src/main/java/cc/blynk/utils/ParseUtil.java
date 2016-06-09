package cc.blynk.utils;

import cc.blynk.server.core.protocol.enums.Response;
import cc.blynk.server.core.protocol.exceptions.BaseServerException;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 1/31/2015.
 */
public final class ParseUtil {

    public static int parseInt(String intString) {
        try {
            return Integer.parseInt(intString);
        } catch (NumberFormatException ex) {
            throw new BaseServerException(intString + " not a valid int number. " + ex.getMessage(), Response.ILLEGAL_COMMAND);
        }
    }

    public static byte parseByte(String byteString) {
        try {
            return Byte.parseByte(byteString);
        } catch (NumberFormatException ex) {
            throw new BaseServerException(byteString + " not a valid byte number.", Response.ILLEGAL_COMMAND);
        }
    }

    public static long parseLong(String longString) {
        try {
            return Long.parseLong(longString);
        } catch (NumberFormatException nfe) {
            throw new BaseServerException(longString + " not a valid long number.", Response.ILLEGAL_COMMAND);
        }
    }

}
