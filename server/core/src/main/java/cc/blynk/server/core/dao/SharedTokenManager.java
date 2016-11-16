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
class SharedTokenManager extends TokenManagerBase {

    private static final Logger log = LogManager.getLogger(SharedTokenManager.class);

    public SharedTokenManager(Iterable<User> users) {
        super(new ConcurrentHashMap<String, TokenValue>() {{
            for (User user : users) {
                for (Map.Entry<Integer, String> entry : user.dashShareTokens.entrySet()) {
                    put(entry.getValue(), new TokenValue(user, entry.getKey().intValue()));
                }
            }
        }});
    }

    public void assignToken(User user, int dashboardId, String newToken) {
        // Clean old token from cache if exists.
        String oldToken = user.dashShareTokens.get(dashboardId);
        if (oldToken != null) {
            cache.remove(oldToken);
        }

        //assign new token
        cleanTokensForNonExistentDashes(user, user.dashShareTokens);
        user.dashShareTokens.put(dashboardId, newToken);
        user.lastModifiedTs = System.currentTimeMillis();

        cache.put(newToken, new TokenValue(user, dashboardId));

        printMessage(user.name, dashboardId, newToken);
    }

    @Override
    String deleteProject(User user, Integer projectId) {
        String removedToken = user.dashShareTokens.remove(projectId);
        if (removedToken != null) {
            cache.remove(removedToken);
            log.info("Deleted {} shared token.", removedToken);
        }
        return removedToken;
    }

    @Override
    void printMessage(String username, int dashId, String token) {
        log.info("Generated shared token for user {} and dashId {} is {}.", username, dashId, token);
    }
}
