package cc.blynk.server.db.model;

import java.util.Date;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 03.03.16.
 */
public class FlashedToken {

    public final String token;

    public final String appId;

    public final String email;

    public final int dashId;

    public final int deviceId;

    public boolean isActivated;

    public Date ts;

    public FlashedToken(String email, String token, String appId, int dashId, int deviceId) {
        this.email = email;
        this.token = token;
        this.appId = appId;
        this.dashId = dashId;
        this.deviceId = deviceId;
    }

    public FlashedToken(String token, String appId, String email, int dashId,
                        int deviceId, boolean isActivated, Date ts) {
        this.token = token;
        this.appId = appId;
        this.email = email;
        this.dashId = dashId;
        this.deviceId = deviceId;
        this.isActivated = isActivated;
        this.ts = ts;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof FlashedToken)) {
            return false;
        }

        FlashedToken that = (FlashedToken) o;

        if (deviceId != that.deviceId) {
            return false;
        }
        if (token != null ? !token.equals(that.token) : that.token != null) {
            return false;
        }
        return !(appId != null ? !appId.equals(that.appId) : that.appId != null);

    }

    @Override
    public int hashCode() {
        int result = token != null ? token.hashCode() : 0;
        result = 31 * result + (appId != null ? appId.hashCode() : 0);
        result = 31 * result + deviceId;
        return result;
    }
}
