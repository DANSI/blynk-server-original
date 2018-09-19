package cc.blynk.server.core.dao;

import cc.blynk.server.core.model.DashBoard;
import cc.blynk.server.core.model.auth.User;
import cc.blynk.server.core.model.device.Device;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 16.11.16.
 */
public class TokenValue {

    public final User user;

    public final DashBoard dash;

    public final Device device;

    public TokenValue(User user, DashBoard dash, Device device) {
        this.user = user;
        this.dash = dash;
        this.device = device;
    }

    public boolean isTemporary() {
        return false;
    }

    public boolean isExpired(long now) {
        return false;
    }

}
