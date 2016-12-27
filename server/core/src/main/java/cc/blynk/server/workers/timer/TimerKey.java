package cc.blynk.server.workers.timer;

import cc.blynk.server.core.dao.UserKey;
import cc.blynk.server.core.model.widgets.controls.Timer;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 27.12.16.
 */
public class TimerKey {

    public final UserKey userKey;

    public final Timer timer;

    public final int dashId;

    public final TimerType type;

    public TimerKey(UserKey userKey, Timer timer, int dashId, TimerType type) {
        this.userKey = userKey;
        this.timer = timer;
        this.dashId = dashId;
        this.type = type;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TimerKey)) return false;

        TimerKey timerKey = (TimerKey) o;

        if (dashId != timerKey.dashId) return false;
        if (userKey != null ? !userKey.equals(timerKey.userKey) : timerKey.userKey != null) return false;
        if (timer != null ? !timer.equals(timerKey.timer) : timerKey.timer != null) return false;
        return type == timerKey.type;

    }

    @Override
    public int hashCode() {
        int result = userKey != null ? userKey.hashCode() : 0;
        result = 31 * result + (timer != null ? timer.hashCode() : 0);
        result = 31 * result + dashId;
        result = 31 * result + (type != null ? type.hashCode() : 0);
        return result;
    }
}
