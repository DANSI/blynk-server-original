package cc.blynk.server.dao;

import cc.blynk.server.model.auth.User;

import java.util.Map;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 22.09.15.
 */
public class SharedTokenManager extends TokenManagerBase {

    public SharedTokenManager(Iterable<User> users) {
        super(users);
    }

    @Override
    Map<Integer, String> getTokens(User user) {
        return user.dashShareTokens;
    }
}
