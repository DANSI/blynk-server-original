package cc.blynk.server.core.dao;

import cc.blynk.server.core.model.auth.User;

import java.util.Collection;
import java.util.concurrent.ConcurrentMap;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 14.10.16.
 */
public class TokenManager {

    private final TokenManagerBase regularTokenManager;
    private final TokenManagerBase sharedTokenManager;

    public TokenManager(ConcurrentMap<UserKey, User> users) {
        Collection<User> allUsers = users.values();
        this.regularTokenManager = new RegularTokenManager(allUsers);
        this.sharedTokenManager = new SharedTokenManager(allUsers);
    }

    public void deleteProject(User user, Integer projectId) {
        regularTokenManager.deleteProject(user, projectId);
        sharedTokenManager.deleteProject(user, projectId);
    }

    public User getUserByToken(String token) {
        return regularTokenManager.getUserByToken(token);
    }

    public User getUserBySharedToken(String token) {
        return sharedTokenManager.getUserByToken(token);
    }

    public String getToken(User user, int projectId) {
        return regularTokenManager.getToken(user, projectId);
    }

    public String getSharedToken(User user, int projectId) {
        return sharedTokenManager.getToken(user, projectId);
    }

    public String refreshToken(User user, int dashId) {
        return regularTokenManager.refreshToken(user, dashId, user.dashTokens);
    }

    public String refreshSharedToken(User user, int dashId) {
        return sharedTokenManager.refreshToken(user, dashId, user.dashShareTokens);
    }
}
