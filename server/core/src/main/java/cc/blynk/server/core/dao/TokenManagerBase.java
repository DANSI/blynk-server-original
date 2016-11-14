package cc.blynk.server.core.dao;

import cc.blynk.server.core.model.auth.User;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 22.09.15.
 */
abstract class TokenManagerBase {

    protected final ConcurrentMap<String, User> cache;

    public TokenManagerBase(ConcurrentMap<String, User> data) {
        this.cache = data;
    }

    public String assignToken(User user, Integer dashboardId, String newToken, ConcurrentMap<Integer, String> tokens) {
        // Clean old token from cache if exists.
        String oldToken = tokens.get(dashboardId);
        if (oldToken != null) {
            cache.remove(oldToken);
        }

        //assign new token
        cleanTokensForNonExistentDashes(user, tokens);
        tokens.put(dashboardId, newToken);
        user.lastModifiedTs = System.currentTimeMillis();

        cache.put(newToken, user);

        printMessage(user.name, dashboardId, newToken);
        return newToken;
    }

    private static void cleanTokensForNonExistentDashes(User user, Map<Integer, String> tokens) {
        Iterator<Integer> iterator = tokens.keySet().iterator();
        while (iterator.hasNext()) {
            if (!user.dashIdExists(iterator.next())) {
                iterator.remove();
            }
        }
    }

    public User getUserByToken(String token) {
        return cache.get(token);
    }

    abstract String deleteProject(User user, Integer projectId);

    abstract void printMessage(String username, Integer dashId, String token);

}
