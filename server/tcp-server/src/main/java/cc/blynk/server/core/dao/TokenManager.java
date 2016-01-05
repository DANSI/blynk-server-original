package cc.blynk.server.core.dao;

import cc.blynk.server.core.model.auth.User;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Map;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 22.09.15.
 */
public class TokenManager extends TokenManagerBase {

    private static final Logger log = LogManager.getLogger(TokenManager.class);

    public TokenManager(Iterable<User> users) {
        super(users);
    }

    @Override
    Map<Integer, String> getTokens(User user) {
        return user.dashTokens;
    }

    @Override
    void printMessage(String username, Integer dashId, String token) {
        log.info("Generated token for user {} and dashId {} is {}.", username, dashId, token);
    }
}
