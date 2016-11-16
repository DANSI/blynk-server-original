package cc.blynk.server.core.dao;

import cc.blynk.server.core.model.auth.User;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 22.09.15.
 */
class RegularTokenManager extends TokenManagerBase {

    private static final Logger log = LogManager.getLogger(RegularTokenManager.class);

    public RegularTokenManager(Iterable<User> users) {
        super(new ConcurrentHashMap<String, TokenValue>() {{
            for (User user : users) {
                for (Map.Entry<Integer, String> entry : user.dashTokens.entrySet()) {
                    put(entry.getValue(), new TokenValue(user, entry.getKey().intValue()));
                }
            }
        }});
    }

    public void assignToken(User user, int dashboardId, String newToken) {
        // Clean old token from cache if exists.
        String oldToken = user.dashTokens.get(dashboardId);
        if (oldToken != null) {
            cache.remove(oldToken);
        }

        //assign new token
        cleanTokensForNonExistentDashes(user, user.dashTokens);
        user.dashTokens.put(dashboardId, newToken);
        user.lastModifiedTs = System.currentTimeMillis();

        cache.put(newToken, new TokenValue(user, dashboardId));

        printMessage(user.name, dashboardId, newToken);
    }

    @Override
    String deleteProject(User user, Integer projectId) {
        String removedToken = user.dashTokens.remove(projectId);
        if (removedToken != null) {
            cache.remove(removedToken);
            log.debug("Deleted {} token.", removedToken);
        }
        return removedToken;
    }

    @Override
    void printMessage(String username, int dashId, String token) {
        log.debug("Generated token for user {} and dashId {} is {}.", username, dashId, token);
    }
}
