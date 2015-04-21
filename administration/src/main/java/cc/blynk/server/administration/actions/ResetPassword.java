package cc.blynk.server.administration.actions;

import cc.blynk.server.core.administration.Executable;
import cc.blynk.server.dao.SessionsHolder;
import cc.blynk.server.dao.UserRegistry;
import cc.blynk.server.model.auth.User;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 21.04.15.
 */
public class ResetPassword implements Executable {

    private static final Logger log = LogManager.getLogger(ResetPassword.class);

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
                user.setPass(SHA256Util.makeHash(newPass, userName));
                user.setLastModifiedTs(System.currentTimeMillis());
                result.add("Password updated.\n");
            }


        }
        result.add("ok\n");
        return result;
    }
}
