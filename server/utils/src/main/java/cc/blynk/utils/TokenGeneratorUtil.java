package cc.blynk.utils;

import java.util.UUID;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 12.05.16.
 */
public final class TokenGeneratorUtil {

    private TokenGeneratorUtil() {
    }

    public static String generateNewToken() {
        return UUID.randomUUID().toString().replace("-", "");
    }

}
