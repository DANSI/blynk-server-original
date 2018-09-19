package cc.blynk.utils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 22.04.15.
 */
public final class SHA256Util {

    private static final String SHA_256 = "SHA-256";

    private SHA256Util() {
        try {
            MessageDigest.getInstance(SHA_256);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not supported.");
        }
    }

    public static String makeHash(String password, String salt) {
        try {
            MessageDigest md = MessageDigest.getInstance(SHA_256);
            md.update(password.getBytes(StandardCharsets.UTF_8));
            byte[] byteData = md.digest(makeHash(salt.toLowerCase()));
            return Base64.getEncoder().encodeToString(byteData);
        } catch (NoSuchAlgorithmException e) {
            //ignore, will never happen.
        }
        return password;
    }

    private static byte[] makeHash(String val) throws NoSuchAlgorithmException {
        return MessageDigest.getInstance(SHA_256).digest(val.getBytes(StandardCharsets.UTF_8));
    }

}
