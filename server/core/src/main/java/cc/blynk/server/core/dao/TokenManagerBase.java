package cc.blynk.server.core.dao;

import cc.blynk.server.core.model.auth.User;
import cc.blynk.utils.TokenGeneratorUtil;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 22.09.15.
 */
public abstract class TokenManagerBase {

    protected final ConcurrentMap<String, User> cache;

    public TokenManagerBase(Iterable<User> users) {
        this.cache = initTokenCache(users);
    }

    private ConcurrentMap<String, User> initTokenCache(Iterable<User> users) {
        return new ConcurrentHashMap<String, User>() {{
            for (User user : users) {
                for (String userToken : getTokens(user).values()) {
                    put(userToken, user);
                }
            }
        }};
    }

    public String getToken(User user, Integer dashboardId) {
        Map<Integer, String> tokens = getTokens(user);
        String token = tokens.get(dashboardId);

        //if token not exists. generate new one
        if (token == null) {
            token = refreshToken(user, dashboardId, tokens);
        }

        return token;
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

    abstract Map<Integer, String> getTokens(User user);

    abstract void deleteProject(User user, Integer projectId);

    abstract void printMessage(String username, Integer dashId, String token);

}
