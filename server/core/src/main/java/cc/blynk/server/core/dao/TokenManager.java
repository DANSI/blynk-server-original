package cc.blynk.server.core.dao;

import cc.blynk.server.core.model.DashBoard;
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

    public String[] deleteProject(User user, DashBoard dash) {
        sharedTokenManager.deleteProject(user, dash.id);
        return regularTokenManager.deleteProject(dash);
    }

    public TokenValue getUserByToken(String token) {
        return regularTokenManager.getUserByToken(token);
    }

    public SharedTokenValue getUserBySharedToken(String token) {
        return sharedTokenManager.getUserByToken(token);
    }

    public void assignToken(User user, int dashId, int deviceId, String newToken) {
        regularTokenManager.assignToken(user, dashId, deviceId, newToken);
    }

    public String refreshToken(User user, int dashId, int deviceId) {
        final String newToken = TokenGeneratorUtil.generateNewToken();
        assignToken(user, dashId, deviceId, newToken);
        return newToken;
    }

    public String refreshSharedToken(User user, int dashId) {
        final String newToken = TokenGeneratorUtil.generateNewToken();
        sharedTokenManager.assignToken(user, dashId, newToken);
        return newToken;
    }
}
