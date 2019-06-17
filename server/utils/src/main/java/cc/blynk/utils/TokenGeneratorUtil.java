package cc.blynk.utils;

import java.security.SecureRandom;
import java.util.Base64;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 12.05.16.
 */
public final class TokenGeneratorUtil {

    private static final SecureRandom secureRandom = new SecureRandom();
    private static final Base64.Encoder base64Encoder = Base64.getUrlEncoder();

    private TokenGeneratorUtil() {
    }

    public static String generateNewToken() {
        byte[] randomBytes = new byte[24];
        secureRandom.nextBytes(randomBytes);
        return base64Encoder.encodeToString(randomBytes);
    }

}
