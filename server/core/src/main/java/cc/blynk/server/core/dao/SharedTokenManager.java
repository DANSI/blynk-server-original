package cc.blynk.server.core.dao;

import cc.blynk.server.core.model.auth.User;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Map;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 22.09.15.
 */
public class SharedTokenManager extends TokenManagerBase {

    private static final Logger log = LogManager.getLogger(SharedTokenManager.class);

    public SharedTokenManager(Iterable<User> users) {
        super(users);
    }

    @Override
    Map<Integer, String> getTokens(User user) {
        return user.dashShareTokens;
    }

    @Override
    void deleteProject(User user, Integer projectId) {
        if (user.dashShareTokens != null) {
            String removedToken = user.dashShareTokens.remove(projectId);
            if (removedToken != null) {
                cache.remove(removedToken);
                log.info("Deleted {} shared token.", removedToken);
            }
        }
    }

    @Override
    void printMessage(String username, Integer dashId, String token) {
        log.info("Generated shared token for user {} and dashId {} is {}.", username, dashId, token);
    }
}
