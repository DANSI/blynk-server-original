package cc.blynk.server.core.dao;

import cc.blynk.server.core.model.auth.User;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 22.09.15.
 */
class SharedTokenManager {

    private final ConcurrentMap<String, SharedTokenValue> cache;

    private static final Logger log = LogManager.getLogger(SharedTokenManager.class);

    public SharedTokenManager(Iterable<User> users) {
        this.cache = new ConcurrentHashMap<String, SharedTokenValue>() {{
            for (User user : users) {
                for (Map.Entry<Integer, String> entry : user.dashShareTokens.entrySet()) {
                    put(entry.getValue(), new SharedTokenValue(user, entry.getKey().intValue()));
                }
            }
        }};
    }

    public void assignToken(User user, int dashId, String newToken) {
        // Clean old token from cache if exists.
        String oldToken = user.dashShareTokens.get(dashId);
        if (oldToken != null) {
            cache.remove(oldToken);
        }

        //assign new token
        cleanTokensForNonExistentDashes(user, user.dashShareTokens);
        user.dashShareTokens.put(dashId, newToken);
        user.lastModifiedTs = System.currentTimeMillis();

        cache.put(newToken, new SharedTokenValue(user, dashId));

        log.info("Generated shared token for user {} and dashId {} is {}.", user.name, dashId, newToken);
    }

    private static void cleanTokensForNonExistentDashes(User user, Map<Integer, String> tokens) {
        Iterator<Integer> iterator = tokens.keySet().iterator();
        while (iterator.hasNext()) {
            if (!user.dashIdExists(iterator.next())) {
                iterator.remove();
            }
        }
    }

    public SharedTokenValue getUserByToken(String token) {
        return cache.get(token);
    }

    String deleteProject(User user, int projectId) {
        String removedToken = user.dashShareTokens.remove(projectId);
        if (removedToken != null) {
            cache.remove(removedToken);
            log.info("Deleted {} shared token.", removedToken);
        }
        return removedToken;
    }

}
