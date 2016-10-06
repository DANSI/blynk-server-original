package cc.blynk.server.core.session;

import cc.blynk.server.core.dao.UserKey;
import cc.blynk.server.core.model.auth.User;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 05.01.16.
 */
public abstract class StateHolder {

    public final User user;
    public final UserKey userKey;

    public StateHolder(User user) {
        this.user = user;
        this.userKey = new UserKey(user);
    }

    public abstract boolean contains(String sharedToken);

}
