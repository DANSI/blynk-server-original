package cc.blynk.server.application.handlers.main.auth;

import cc.blynk.server.core.model.auth.User;
import cc.blynk.server.core.session.StateHolder;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 13.09.15.
 */
public class AppStateHolder extends StateHolder {

    public final String osType;
    public final String version;

    public AppStateHolder(User user, String osType, String version) {
        super(user);
        this.osType = osType;
        this.version = version;
    }

    @Override
    public boolean contains(String sharedToken) {
        return true;
    }
}
