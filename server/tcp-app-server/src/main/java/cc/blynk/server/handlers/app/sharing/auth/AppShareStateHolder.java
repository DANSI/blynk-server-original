package cc.blynk.server.handlers.app.sharing.auth;

import cc.blynk.server.core.model.auth.User;
import cc.blynk.server.handlers.app.main.auth.AppStateHolder;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 13.09.15.
 */
public class AppShareStateHolder extends AppStateHolder {

    public final String token;
    public final int dashId;

    public AppShareStateHolder(User user, String osType, String version, String token, int dashId) {
        super(user, osType, version);
        this.token = token;
        this.dashId = dashId;
    }

    @Override
    public boolean contains(String sharedToken) {
        return token.equals(sharedToken);
    }

}
