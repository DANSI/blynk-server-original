package cc.blynk.server.internal.token;

import java.io.Serializable;
import java.util.concurrent.TimeUnit;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 12.10.18.
 */
public abstract class BaseToken implements Serializable {

    public final String email;
    private final long expireAt;
    static final long DEFAULT_EXPIRE_TIME = TimeUnit.MINUTES.toMillis(45);

    BaseToken(String email, long tokenExpirationPeriodMillis) {
        this.email = email;
        this.expireAt = System.currentTimeMillis() + tokenExpirationPeriodMillis;
    }

    boolean isExpired(long now) {
        return expireAt < now;
    }
}
