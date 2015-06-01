package cc.blynk.server.core.administration.actions;

import cc.blynk.common.utils.ParseUtil;
import cc.blynk.server.core.administration.Executable;
import cc.blynk.server.dao.SessionsHolder;
import cc.blynk.server.dao.UserRegistry;
import cc.blynk.server.model.auth.User;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * Utility class used in order to find out active users.
 *
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 21.04.15.
 */
public class ActiveUsers implements Executable {

    private static final Logger log = LogManager.getLogger(ActiveUsers.class);

    @Override
    public List<String> execute(UserRegistry userRegistry, SessionsHolder sessionsHolder, String... params) {
        int active = 0;
        int periodDays = 1;
        if (params != null && params.length == 1) {
            periodDays = ParseUtil.parseInt(params[0]);
        }
        long now = System.currentTimeMillis();
        for (User user : userRegistry.getUsers().values()) {
            long diff = now - user.getLastModifiedTs();
            if (diff < periodDays * 24 * 60 * 60 * 1000) {
                active++;
            }
        }

        List<String> result = new ArrayList<>();
        result.add(String.valueOf(active));

        log.info("Active users : {}", active);
        result.add("ok\n");
        return result;
    }

}
