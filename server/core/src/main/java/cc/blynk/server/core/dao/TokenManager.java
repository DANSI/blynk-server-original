package cc.blynk.server.core.dao;

import cc.blynk.server.core.model.DashBoard;
import cc.blynk.server.core.model.auth.User;
import cc.blynk.server.core.model.device.Device;
import cc.blynk.server.db.DBManager;
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
    private final DBManager dbManager;
    private final String host;

    public TokenManager(ConcurrentMap<UserKey, User> users, DBManager dbManager, String host) {
        Collection<User> allUsers = users.values();
        this.regularTokenManager = new RegularTokenManager(allUsers);
        this.sharedTokenManager = new SharedTokenManager(allUsers);
        this.dbManager = dbManager;
        this.host = host;
    }

    public void deleteDevice(Device device) {
        String token = regularTokenManager.deleteDeviceToken(device);
        if (token != null) {
            dbManager.removeToken(token);
        }
    }

    public void deleteDash(DashBoard dash) {
        //todo clear shared token from DB?
        sharedTokenManager.deleteProject(dash);
        String[] removedTokens = regularTokenManager.deleteProject(dash);
        dbManager.removeToken(removedTokens);
    }

    public TokenValue getTokenValueByToken(String token) {
        return regularTokenManager.getUserByToken(token);
    }

    public SharedTokenValue getUserBySharedToken(String token) {
        return sharedTokenManager.getUserByToken(token);
    }

    public void assignToken(User user, DashBoard dash, Device device, String newToken) {
        String oldToken = regularTokenManager.assignToken(user, dash, device, newToken);

        dbManager.assignServerToToken(newToken, host, user.email, dash.id, device.id);
        if (oldToken != null) {
            dbManager.removeToken(oldToken);
        }
    }

    public String refreshToken(User user, DashBoard dash, Device device) {
        String newToken = TokenGeneratorUtil.generateNewToken();
        assignToken(user, dash, device, newToken);
        return newToken;
    }

    public String refreshSharedToken(User user, DashBoard dash) {
        String newToken = TokenGeneratorUtil.generateNewToken();
        sharedTokenManager.assignToken(user, dash, newToken);
        return newToken;
    }

    public void updateRegularCache(String token, User user, DashBoard dash, Device device) {
        regularTokenManager.cache.put(token, new TokenValue(user, dash, device));
    }

    public void updateSharedCache(String token, User user, int dashId) {
        sharedTokenManager.cache.put(token, new SharedTokenValue(user, dashId));
    }
}
