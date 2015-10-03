package cc.blynk.server.handlers.hardware.auth;

import cc.blynk.server.model.auth.User;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 13.09.15.
 */
public class HandlerState {

    public final Integer dashId;
    public final User user;
    public final String token;
    public final String osType;
    public final String version;

    public HandlerState(User user) {
        this(null, user, null);
    }

    public HandlerState(Integer dashId, User user, String token) {
        this.dashId = dashId;
        this.user = user;
        this.token = token;
        this.osType = null;
        this.version = null;
    }


    public HandlerState(User user, String osType, String version) {
        this.dashId = null;
        this.user = user;
        this.token = null;
        this.osType = osType;
        this.version = version;
    }

}
