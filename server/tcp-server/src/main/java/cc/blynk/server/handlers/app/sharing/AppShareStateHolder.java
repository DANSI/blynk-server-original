package cc.blynk.server.handlers.app.sharing;

import cc.blynk.server.handlers.app.main.auth.AppStateHolder;
import cc.blynk.server.model.auth.User;

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

    public boolean contains(String sharedToken) {
        return token.equals(sharedToken);
    }

}
