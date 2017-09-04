package cc.blynk.utils;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 01.11.15.
 */
public final class PinUtil {

    private PinUtil() {
    }

    public static boolean isReadOperation(String body) {
        return body.length() > 1 && body.charAt(1) == 'r';
    }
}
