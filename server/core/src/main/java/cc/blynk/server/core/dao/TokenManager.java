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

    public User getUserByToken(String token) {
        return regularTokenManager.getUserByToken(token);
    }

    public User getUserBySharedToken(String token) {
        return sharedTokenManager.getUserByToken(token);
    }

    public String refreshToken(User user, int dashId) {
        final String newToken = TokenGeneratorUtil.generateNewToken();
        return regularTokenManager.assignToken(user, dashId, newToken, user.dashTokens);
    }

    public String refreshSharedToken(User user, int dashId) {
        final String newToken = TokenGeneratorUtil.generateNewToken();
        return sharedTokenManager.assignToken(user, dashId, newToken, user.dashShareTokens);
    }
}
