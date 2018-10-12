package cc.blynk.server.internal.token;

import java.io.Serializable;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 12.10.18.
 */
public abstract class BaseToken implements Serializable {

    public final String email;
    public final long createdAt;

    public BaseToken(String email) {
        this.email = email;
        this.createdAt = System.currentTimeMillis();
    }
}
