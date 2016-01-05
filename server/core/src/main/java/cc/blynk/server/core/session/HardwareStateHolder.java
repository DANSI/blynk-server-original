package cc.blynk.server.core.session;

import cc.blynk.server.core.model.auth.User;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 13.09.15.
 */
public class HardwareStateHolder extends StateHolder {

    public final int dashId;
    public final String token;

    public HardwareStateHolder(int dashId, User user, String token) {
        super(user);
        this.dashId = dashId;
        this.token = token;
    }

    @Override
    public boolean contains(String sharedToken) {
        return false;
    }
}
