package cc.blynk.server.handlers.app.main.auth;

import cc.blynk.server.model.auth.User;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 13.09.15.
 */
public class AppStateHolder {

    public final User user;
    public final String osType;
    public final String version;

    public AppStateHolder(User user, String osType, String version) {
        this.user = user;
        this.osType = osType;
        this.version = version;
    }

}
