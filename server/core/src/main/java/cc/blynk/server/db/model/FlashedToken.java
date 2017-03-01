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

    public String username;

    public int deviceId;

    public boolean isActivated;

    public Date ts;

    public FlashedToken(String token, String appName, int deviceId) {
        this.token = token;
        this.appName = appName;
        this.deviceId = deviceId;
    }

    public FlashedToken(String token, String appName, String username, int deviceId, boolean isActivated, Date ts) {
        this.token = token;
        this.appName = appName;
        this.username = username;
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
        if (isActivated != that.isActivated) return false;
        if (token != null ? !token.equals(that.token) : that.token != null) return false;
        if (appName != null ? !appName.equals(that.appName) : that.appName != null) return false;
        if (username != null ? !username.equals(that.username) : that.username != null) return false;
        return !(ts != null ? !ts.equals(that.ts) : that.ts != null);

    }

    @Override
    public int hashCode() {
        int result = token != null ? token.hashCode() : 0;
        result = 31 * result + (appName != null ? appName.hashCode() : 0);
        result = 31 * result + (username != null ? username.hashCode() : 0);
        result = 31 * result + deviceId;
        result = 31 * result + (isActivated ? 1 : 0);
        result = 31 * result + (ts != null ? ts.hashCode() : 0);
        return result;
    }
}
