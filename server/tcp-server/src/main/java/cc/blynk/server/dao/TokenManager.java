package cc.blynk.server.dao;

import cc.blynk.server.model.auth.User;

import java.util.Map;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 22.09.15.
 */
public class TokenManager extends TokenManagerBase {

    public TokenManager(Iterable<User> users) {
        super(users);
    }

    @Override
    Map<Integer, String> getTokens(User user) {
        return user.dashTokens;
    }
}
