package cc.blynk.server.core.reporting.raw;

import cc.blynk.server.core.model.auth.User;
import cc.blynk.server.core.model.enums.PinType;
import cc.blynk.server.core.reporting.GraphPinRequest;

import java.io.Serializable;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 20.07.17.
 */
public final class BaseReportingKey implements Serializable {

    public final String email;
    public final String appName;
    public final int dashId;
    public final int deviceId;
    public final PinType pinType;
    public final short pin;

    public BaseReportingKey(User user, GraphPinRequest graphPinRequest) {
        this(user.email, user.appName,
             graphPinRequest.dashId, graphPinRequest.deviceId,
             graphPinRequest.pinType, graphPinRequest.pin);
    }

    public BaseReportingKey(String email, String appName, int dashId, int deviceId, PinType pinType, short pin) {
        this.email = email;
        this.appName = appName;
        this.dashId = dashId;
        this.deviceId = deviceId;
        this.pinType = pinType;
        this.pin = pin;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof BaseReportingKey)) {
            return false;
        }

        BaseReportingKey that = (BaseReportingKey) o;

        if (dashId != that.dashId) {
            return false;
        }
        if (deviceId != that.deviceId) {
            return false;
        }
        if (pin != that.pin) {
            return false;
        }
        if (email != null ? !email.equals(that.email) : that.email != null) {
            return false;
        }
        if (appName != null ? !appName.equals(that.appName) : that.appName != null) {
            return false;
        }
        return pinType == that.pinType;
    }

    @Override
    public int hashCode() {
        int result = email != null ? email.hashCode() : 0;
        result = 31 * result + (appName != null ? appName.hashCode() : 0);
        result = 31 * result + dashId;
        result = 31 * result + deviceId;
        result = 31 * result + (pinType != null ? pinType.hashCode() : 0);
        result = 31 * result + (int) pin;
        return result;
    }
}
