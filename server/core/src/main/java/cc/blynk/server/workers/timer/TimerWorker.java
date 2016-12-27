package cc.blynk.server.workers.timer;

import cc.blynk.server.core.dao.SessionDao;
import cc.blynk.server.core.dao.UserDao;
import cc.blynk.server.core.dao.UserKey;
import cc.blynk.server.core.model.DashBoard;
import cc.blynk.server.core.model.auth.Session;
import cc.blynk.server.core.model.auth.User;
import cc.blynk.server.core.model.widgets.Widget;
import cc.blynk.server.core.model.widgets.controls.Timer;
import cc.blynk.utils.DateTimeUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.LocalTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicReferenceArray;

import static cc.blynk.server.core.protocol.enums.Command.HARDWARE;
import static cc.blynk.server.workers.timer.TimerType.START;
import static cc.blynk.server.workers.timer.TimerType.STOP;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 2/6/2015.
 *
 * Simplest possible timer implementation.
 *
 */
public class TimerWorker implements Runnable {

    private static final Logger log = LogManager.getLogger(TimerWorker.class);

    private final UserDao userDao;
    private final SessionDao sessionDao;
    private final AtomicReferenceArray<ConcurrentMap<TimerKey, String>> timerExecutors;

    public TimerWorker(UserDao userDao, SessionDao sessionDao) {
        this.userDao = userDao;
        this.sessionDao = sessionDao;
        //array cell for every second in a day,
        //yes, it costs at least 350kb of memory, but still cheap :)
        this.timerExecutors = new AtomicReferenceArray<>(86400);
        init(userDao.users);
    }

    private void init(ConcurrentMap<UserKey, User> users) {
        int counter = 0;
        for (Map.Entry<UserKey, User> entry : users.entrySet()) {
            for (DashBoard dashBoard : entry.getValue().profile.dashBoards) {
                for (Widget widget : dashBoard.widgets) {
                    if (widget instanceof Timer) {
                        Timer timer = (Timer) widget;
                        counter++;
                        add(entry.getKey(), timer, dashBoard.id);
                    }
                }
            }
        }
        log.info("Timers : {}", counter);
    }

    public void add(UserKey userKey, Timer timer, int dashId) {
        if (timer.isValidStart()) {
            ConcurrentMap<TimerKey, String> timers = get(timer.startTime);
            timers.put(new TimerKey(userKey, timer, dashId, START), timer.startValue);
        }
        if (timer.isValidStop()) {
            ConcurrentMap<TimerKey, String> timers = get(timer.stopTime);
            timers.put(new TimerKey(userKey, timer, dashId, STOP), timer.stopValue);
        }
    }

    public void delete(UserKey userKey, Timer timer, int dashId) {
        if (timer.isValidStart()) {
            ConcurrentMap<TimerKey, String> timers = get(timer.startTime);
            timers.remove(new TimerKey(userKey, timer, dashId, START));
        }
        if (timer.isValidStop()) {
            ConcurrentMap<TimerKey, String> timers = get(timer.stopTime);
            timers.remove(new TimerKey(userKey, timer, dashId, STOP));
        }
    }

    /**
     * Here is small chance that few threads will add timer for same time.
     * So doing it threadsafe.
     */
    private ConcurrentMap<TimerKey, String> get(int time) {
        ConcurrentMap<TimerKey, String> timers = timerExecutors.get(time);
        if (timers == null) {
            ConcurrentMap<TimerKey, String>  update = new ConcurrentHashMap<>();
            if (timerExecutors.compareAndSet(time, null, update)) {
                return update;
            }
            return timerExecutors.get(time);
        }
        return timers;
    }

    @Override
    public void run() {
        log.trace("Starting timer...");

        int curSeconds = LocalTime.now(DateTimeUtils.UTC).toSecondOfDay();
        ConcurrentMap<TimerKey, String> tickedExecutors = timerExecutors.get(curSeconds);

        if (tickedExecutors != null) {
            log.trace("222");
        }

        if (tickedExecutors == null || tickedExecutors.size() == 0) {
            return;
        }

        int tickedTimers = 0;

        for (Map.Entry<TimerKey, String> entry : tickedExecutors.entrySet()) {
            final TimerKey key = entry.getKey();
            User user = userDao.users.get(key.userKey);
            if (user != null) {
                DashBoard dash = user.profile.getDashById(key.dashId);
                if (dash != null && dash.isActive) {
                    tickedTimers++;
                    final String value = entry.getValue();
                    triggerTimer(sessionDao, key.userKey, value, key.dashId, key.timer.deviceId);
                    key.timer.value = value;
                }
            }
        }

        //logging only events when timers ticked.
        log.info("Timer finished. Ticked {} timers.", tickedTimers);
    }

    private void triggerTimer(SessionDao sessionDao, UserKey userKey, String value, int dashId, int deviceId) {
        Session session = sessionDao.userSession.get(userKey);
        if (session != null) {
            session.sendMessageToHardware(dashId, HARDWARE, 7777, value, deviceId);
        }
    }

}
