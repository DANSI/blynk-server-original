package cc.blynk.server.core.dao;

import cc.blynk.server.core.model.auth.User;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 16.11.16.
 */
public final class TokenValue {

    public final User user;

    public final int dashId;

    public TokenValue(User user, int dashId) {
        this.user = user;
        this.dashId = dashId;
    }
}
