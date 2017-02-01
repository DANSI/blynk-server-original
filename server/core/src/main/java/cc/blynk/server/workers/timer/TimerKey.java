package cc.blynk.server.workers.timer;

import cc.blynk.server.core.dao.UserKey;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 27.12.16.
 */
public class TimerKey {

    public final UserKey userKey;

    public final int dashId;

    public final int deviceId;

    public final long widgetId;

    public final int exactlyTime;

    public final String value;

    public TimerKey(UserKey userKey, int dashId, int deviceId, long widgetId, int exactlyTime, String value) {
        this.userKey = userKey;
        this.dashId = dashId;
        this.deviceId = deviceId;
        this.widgetId = widgetId;
        this.exactlyTime = exactlyTime;
        this.value = value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TimerKey)) return false;

        TimerKey timerKey = (TimerKey) o;

        if (dashId != timerKey.dashId) return false;
        if (deviceId != timerKey.deviceId) return false;
        if (widgetId != timerKey.widgetId) return false;
        if (exactlyTime != timerKey.exactlyTime) return false;
        if (userKey != null ? !userKey.equals(timerKey.userKey) : timerKey.userKey != null) return false;
        return !(value != null ? !value.equals(timerKey.value) : timerKey.value != null);

    }

    @Override
    public int hashCode() {
        int result = userKey != null ? userKey.hashCode() : 0;
        result = 31 * result + dashId;
        result = 31 * result + deviceId;
        result = 31 * result + (int) (widgetId ^ (widgetId >>> 32));
        result = 31 * result + exactlyTime;
        result = 31 * result + (value != null ? value.hashCode() : 0);
        return result;
    }
}
