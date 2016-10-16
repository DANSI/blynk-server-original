package cc.blynk.server.core.dao;

import cc.blynk.server.core.model.auth.User;
import cc.blynk.utils.TokenGeneratorUtil;

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

    public String refreshToken(User user, Integer dashboardId, Map<Integer, String> dashTokens) {
        // Clean old token from cache if exists.
        String oldToken = dashTokens.get(dashboardId);
        if (oldToken != null) cache.remove(oldToken);

        //Create new token
        String newToken = TokenGeneratorUtil.generateNewToken();
        user.putToken(dashboardId, newToken, dashTokens);
        cache.put(newToken, user);

        printMessage(user.name, dashboardId, newToken);
        return newToken;
    }

    public User getUserByToken(String token) {
        return cache.get(token);
    }

    public abstract Map<Integer, String> getTokens(User user);

    abstract String deleteProject(User user, Integer projectId);

    abstract void printMessage(String username, Integer dashId, String token);

}
