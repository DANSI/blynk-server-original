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

    public final int additionalId;

    public final int exactlyTime;

    public TimerKey(UserKey userKey, int dashId, int deviceId, long widgetId, int additionalId, int exactlyTime) {
        this.userKey = userKey;
        this.dashId = dashId;
        this.deviceId = deviceId;
        this.widgetId = widgetId;
        this.additionalId = additionalId;
        this.exactlyTime = exactlyTime;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TimerKey)) return false;

        TimerKey timerKey = (TimerKey) o;

        if (dashId != timerKey.dashId) return false;
        if (deviceId != timerKey.deviceId) return false;
        if (widgetId != timerKey.widgetId) return false;
        if (additionalId != timerKey.additionalId) return false;
        if (exactlyTime != timerKey.exactlyTime) return false;
        return !(userKey != null ? !userKey.equals(timerKey.userKey) : timerKey.userKey != null);

    }

    @Override
    public int hashCode() {
        int result = userKey != null ? userKey.hashCode() : 0;
        result = 31 * result + dashId;
        result = 31 * result + deviceId;
        result = 31 * result + (int) (widgetId ^ (widgetId >>> 32));
        result = 31 * result + additionalId;
        result = 31 * result + exactlyTime;
        return result;
    }
}
