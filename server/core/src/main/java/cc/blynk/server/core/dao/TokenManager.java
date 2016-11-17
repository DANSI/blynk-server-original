package cc.blynk.server.core.dao;

import cc.blynk.server.core.BlockingIOProcessor;
import cc.blynk.server.core.model.DashBoard;
import cc.blynk.server.core.model.auth.User;
import cc.blynk.server.redis.RedisClient;
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
    private final RedisClient redisClient;
    private final String currentIp;

    public TokenManager(ConcurrentMap<UserKey, User> users, BlockingIOProcessor blockingIOProcessor, RedisClient redisClient, String currentIp) {
        Collection<User> allUsers = users.values();
        this.regularTokenManager = new RegularTokenManager(allUsers);
        this.sharedTokenManager = new SharedTokenManager(allUsers);
        this.blockingIOProcessor = blockingIOProcessor;
        this.redisClient = redisClient;
        this.currentIp = currentIp;
    }

    public void deleteProject(User user, DashBoard dash) {
        sharedTokenManager.deleteProject(user, dash.id);
        String[] removedTokens = regularTokenManager.deleteProject(dash);

        if (removedTokens.length > 0) {
            blockingIOProcessor.execute(() -> {
                redisClient.removeToken(removedTokens);
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

        blockingIOProcessor.execute(() -> {
            redisClient.assignServerToToken(newToken, currentIp);
            if (oldToken != null) {
                redisClient.removeToken(oldToken);
            }
        });
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
