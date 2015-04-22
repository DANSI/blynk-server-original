package cc.blynk.server.core.administration.actions;

import cc.blynk.server.core.administration.Executable;
import cc.blynk.server.dao.SessionsHolder;
import cc.blynk.server.dao.UserRegistry;
import cc.blynk.server.model.auth.User;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 21.04.15.
 */
public class ResetPassword implements Executable {

    private static final Logger log = LogManager.getLogger(ResetPassword.class);

    private static String makeHash(String password, String salt) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(password.getBytes("UTF-8"));

            byte byteData[] = md.digest(makeHash(salt.toLowerCase()));

            return Base64.getEncoder().encodeToString(byteData);
        } catch (NoSuchAlgorithmException | UnsupportedEncodingException e) {
            log.error("Unable to make hash for pass. No hashing.", e);
        }

        return password;
    }

    private static byte[] makeHash(String val) throws NoSuchAlgorithmException, UnsupportedEncodingException {
        return MessageDigest.getInstance("SHA-256").digest(val.getBytes("UTF-8"));
    }

    @Override
    public List<String> execute(UserRegistry userRegistry, SessionsHolder sessionsHolder, String... params) {
        List<String> result = new ArrayList<>();
        if (params != null && params.length == 2) {
            String userName = params[0];
            String newPass = params[1];

            User user = userRegistry.getByName(userName);
            if (user == null) {
                log.error("User '{}' not exists.", userName);
            } else {
                user.setPass(makeHash(newPass, userName));
                user.setLastModifiedTs(System.currentTimeMillis());
                result.add("Password updated.\n");
            }
        }
        result.add("ok\n");
        return result;
    }

}
