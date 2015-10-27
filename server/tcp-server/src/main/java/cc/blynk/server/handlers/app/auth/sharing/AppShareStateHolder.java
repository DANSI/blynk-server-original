package cc.blynk.server.handlers.app.auth.sharing;

import cc.blynk.server.handlers.app.auth.AppStateHolder;
import cc.blynk.server.model.auth.User;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 13.09.15.
 */
public class AppShareStateHolder extends AppStateHolder {

    public final String token;

    public AppShareStateHolder(User user, String osType, String version, String token) {
        super(user, osType, version);
        this.token = token;
    }

    public boolean contains(String sharedToken) {
        return token.equals(sharedToken);
    }

}
