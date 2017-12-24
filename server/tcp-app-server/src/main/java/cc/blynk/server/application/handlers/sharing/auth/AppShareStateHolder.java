package cc.blynk.server.application.handlers.sharing.auth;

import cc.blynk.server.application.handlers.main.auth.AppStateHolder;
import cc.blynk.server.application.handlers.main.auth.Version;
import cc.blynk.server.core.model.auth.User;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 13.09.15.
 */
public final class AppShareStateHolder extends AppStateHolder {

    public final String token;
    public final int dashId;

    AppShareStateHolder(User user, Version version, String token, int dashId) {
        super(user, version);
        this.token = token;
        this.dashId = dashId;
    }

    @Override
    public boolean contains(String sharedToken) {
        return sharedToken != null && token.equals(sharedToken);
    }

    @Override
    public boolean isSameDash(int inDashId) {
        return this.dashId == inDashId;
    }

}
