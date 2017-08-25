package cc.blynk.server.core.dao;

import cc.blynk.server.core.model.DashBoard;
import cc.blynk.server.core.model.auth.User;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 16.11.16.
 */
public final class TokenValue {

    public final User user;

    public final DashBoard dash;

    public final int deviceId;

    public TokenValue(User user, DashBoard dash, int deviceId) {
        this.user = user;
        this.dash = dash;
        this.deviceId = deviceId;
    }
}
