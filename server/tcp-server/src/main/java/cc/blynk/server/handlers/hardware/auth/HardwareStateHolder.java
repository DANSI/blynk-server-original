package cc.blynk.server.handlers.hardware.auth;

import cc.blynk.server.model.auth.User;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 13.09.15.
 */
public class HardwareStateHolder {

    public final Integer dashId;
    public final User user;
    public final String token;
    public final String osType;
    public final String version;

    public HardwareStateHolder(User user) {
        this(null, user, null);
    }

    public HardwareStateHolder(Integer dashId, User user, String token) {
        this.dashId = dashId;
        this.user = user;
        this.token = token;
        this.osType = null;
        this.version = null;
    }


    public HardwareStateHolder(User user, String osType, String version) {
        this.dashId = null;
        this.user = user;
        this.token = null;
        this.osType = osType;
        this.version = version;
    }

    public boolean isOldAPI() {
        return version == null;
    }

}
