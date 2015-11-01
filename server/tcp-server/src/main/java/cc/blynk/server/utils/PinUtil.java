package cc.blynk.server.utils;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 01.11.15.
 */
public class PinUtil {

    public static boolean pinModeMessage(String body) {
        return body.length() > 0 && body.charAt(0) == 'p';
    }

    public static boolean isWriteOperation(String body) {
        return body.length() > 1 && body.charAt(1) == 'w';
    }
}
