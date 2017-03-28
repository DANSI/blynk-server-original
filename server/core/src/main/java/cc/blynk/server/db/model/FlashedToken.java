package cc.blynk.server.db.model;

import java.util.Date;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 03.03.16.
 */
public class FlashedToken {

    public String token;

    public String appName;

    public String email;

    public int deviceId;

    public boolean isActivated;

    public Date ts;

    public FlashedToken(String token, String appName, int deviceId) {
        this.token = token;
        this.appName = appName;
        this.deviceId = deviceId;
    }

    public FlashedToken(String token, String appName, String email, int deviceId, boolean isActivated, Date ts) {
        this.token = token;
        this.appName = appName;
        this.email = email;
        this.deviceId = deviceId;
        this.isActivated = isActivated;
        this.ts = ts;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof FlashedToken)) return false;

        FlashedToken that = (FlashedToken) o;

        if (deviceId != that.deviceId) return false;
        if (token != null ? !token.equals(that.token) : that.token != null) return false;
        return !(appName != null ? !appName.equals(that.appName) : that.appName != null);

    }

    @Override
    public int hashCode() {
        int result = token != null ? token.hashCode() : 0;
        result = 31 * result + (appName != null ? appName.hashCode() : 0);
        result = 31 * result + deviceId;
        return result;
    }
}
