package cc.blynk.server.core.dao;

import cc.blynk.server.core.model.AppName;
import cc.blynk.server.core.model.auth.User;

/**
 * User key for session. Holds user email and app user belongs to.
 *
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 17.05.16.
 */
public final class UserKey {

    public final String email;

    public final String appName;

    public UserKey(User user) {
        this(user.email, user.appName);
    }

    public UserKey(String email, String appName) {
        this.email = email;
        if (appName == null) {
            this.appName = AppName.BLYNK;
        } else {
            this.appName = appName;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        UserKey userKey = (UserKey) o;

        if (email != null ? !email.equals(userKey.email) : userKey.email != null) return false;
        return !(appName != null ? !appName.equals(userKey.appName) : userKey.appName != null);

    }

    @Override
    public int hashCode() {
        int result = email != null ? email.hashCode() : 0;
        result = 31 * result + (appName != null ? appName.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "UserKey{" +
                "email='" + email + '\'' +
                ", appName='" + appName + '\'' +
                '}';
    }
}
