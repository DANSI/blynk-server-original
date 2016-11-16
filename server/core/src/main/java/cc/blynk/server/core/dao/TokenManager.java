package cc.blynk.server.core.dao;

import cc.blynk.server.core.model.auth.User;
import cc.blynk.utils.TokenGeneratorUtil;

import java.util.Collection;
import java.util.concurrent.ConcurrentMap;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 14.10.16.
 */
public class TokenManager {

    private final RegularTokenManager regularTokenManager;
    private final SharedTokenManager sharedTokenManager;

    public TokenManager(ConcurrentMap<UserKey, User> users) {
        Collection<User> allUsers = users.values();
        this.regularTokenManager = new RegularTokenManager(allUsers);
        this.sharedTokenManager = new SharedTokenManager(allUsers);
    }

    public String deleteProject(User user, Integer projectId) {
        sharedTokenManager.deleteProject(user, projectId);
        return regularTokenManager.deleteProject(user, projectId);
    }

    public TokenValue getUserByToken(String token) {
        return regularTokenManager.getUserByToken(token);
    }

    public TokenValue getUserBySharedToken(String token) {
        return sharedTokenManager.getUserByToken(token);
    }

    public void assignToken(User user, int dashId, String newToken) {
        regularTokenManager.assignToken(user, dashId, newToken);
    }

    public String refreshToken(User user, int dashId) {
        final String newToken = TokenGeneratorUtil.generateNewToken();
        assignToken(user, dashId, newToken);
        return newToken;
    }

    public String refreshSharedToken(User user, int dashId) {
        final String newToken = TokenGeneratorUtil.generateNewToken();
        sharedTokenManager.assignToken(user, dashId, newToken);
        return newToken;
    }
}
