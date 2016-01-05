package cc.blynk.server.core;

import cc.blynk.server.model.auth.User;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 05.01.16.
 */
public abstract class StateHolder {

    public final User user;

    public StateHolder(User user) {
        this.user = user;
    }

    public abstract boolean contains(String sharedToken);

}
