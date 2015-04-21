package cc.blynk.server.administration.actions;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 26.03.15.
 */
public final class SHA256Util {

    private static final Logger log = LoggerFactory.getLogger(SHA256Util.class);

    public static String makeHash(String password, String salt) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(password.getBytes("UTF-8"));

            byte byteData[] = md.digest(makeHash(salt.toLowerCase()));

            return bytesToHex(byteData);
        } catch (NoSuchAlgorithmException | UnsupportedEncodingException e) {
            log.error("Unable to make hash for pass. No hashing.", e);
        }

        return password;
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte byt : bytes) {
            result.append(Integer.toString((byt & 0xff) + 0x100, 16).substring(1));
        }
        return result.toString();
    }

    private static byte[] makeHash(String val) throws NoSuchAlgorithmException, UnsupportedEncodingException {
        return MessageDigest.getInstance("SHA-256").digest(val.getBytes("UTF-8"));
    }


}
