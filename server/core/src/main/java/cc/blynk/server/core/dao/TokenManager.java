package cc.blynk.server.core.dao;

import cc.blynk.server.core.BlockingIOProcessor;
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
    private final BlockingIOProcessor blockingIOProcessor;
    private final DBManager dbManager;
    private final String currentIp;

    public TokenManager(ConcurrentMap<UserKey, User> users, BlockingIOProcessor blockingIOProcessor, DBManager dbManager, String currentIp) {
        Collection<User> allUsers = users.values();
        this.regularTokenManager = new RegularTokenManager(allUsers);
        this.sharedTokenManager = new SharedTokenManager(allUsers);
        this.blockingIOProcessor = blockingIOProcessor;
        this.dbManager = dbManager;
        this.currentIp = currentIp;
    }

    public void deleteDevice(Device device) {
        String token = regularTokenManager.deleteDeviceToken(device);
        if (token != null) {
            blockingIOProcessor.executeDB(() -> {
                dbManager.removeToken(token);
            });
        }
    }

    public void deleteDash(DashBoard dash) {
        //todo clear shared token from DB?
        sharedTokenManager.deleteProject(dash);
        String[] removedTokens = regularTokenManager.deleteProject(dash);

        if (removedTokens.length > 0) {
            blockingIOProcessor.executeDB(() -> {
                dbManager.removeToken(removedTokens);
            });
        }

    }

    public TokenValue getUserByToken(String token) {
        return regularTokenManager.getUserByToken(token);
    }

    public SharedTokenValue getUserBySharedToken(String token) {
        return sharedTokenManager.getUserByToken(token);
    }

    public void assignToken(User user, int dashId, int deviceId, String newToken) {
        String oldToken = regularTokenManager.assignToken(user, dashId, deviceId, newToken);

        blockingIOProcessor.executeDB(() -> {
            dbManager.assignServerToToken(newToken, currentIp);
            if (oldToken != null) {
                dbManager.removeToken(oldToken);
            }
        });
    }

    public String refreshToken(User user, int dashId, int deviceId) {
        final String newToken = TokenGeneratorUtil.generateNewToken();
        assignToken(user, dashId, deviceId, newToken);
        return newToken;
    }

    public String refreshSharedToken(User user, DashBoard dash) {
        final String newToken = TokenGeneratorUtil.generateNewToken();
        sharedTokenManager.assignToken(user, dash, newToken);
        return newToken;
    }
}
