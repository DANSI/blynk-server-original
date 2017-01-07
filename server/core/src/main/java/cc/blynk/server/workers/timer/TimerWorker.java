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

import static cc.blynk.server.core.protocol.enums.Command.HARDWARE;

/**
 * Timer worker class responsible for triggering all timers at specified time.
 * Current implementation is some kind of Hashed Wheel Timer.
 * In general idea is very simple :
 *
 * Select timers at specified cell timer[secondsOfDayNow]
 * and run it one by one, instead of naive implementation
 * with iteration over all profiles every second
 *
 * + Concurrency around it as timerWorker may be accessed from different threads.
 *
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 2/6/2015.
 *
 */
public class TimerWorker implements Runnable {

    private static final Logger log = LogManager.getLogger(TimerWorker.class);

    private final UserDao userDao;
    private final SessionDao sessionDao;
    private final ConcurrentMap<TimerKey, String>[] timerExecutors;
    private final static int size = 8640;
    private final static String EMPTY_VALUE = "";

    @SuppressWarnings("unchecked")
    public TimerWorker(UserDao userDao, SessionDao sessionDao) {
        this.userDao = userDao;
        this.sessionDao = sessionDao;
        //array cell for every second in a day,
        //yes, it costs a bit of memory, but still cheap :)
        this.timerExecutors = new ConcurrentMap[size];
        for (int i = 0; i < size; i++) {
            timerExecutors[i] = new ConcurrentHashMap<>();
        }
        init(userDao.users);
    }

    private static int hash(int time) {
        return time / 10;
    }

    private void init(ConcurrentMap<UserKey, User> users) {
        int counter = 0;
        for (Map.Entry<UserKey, User> entry : users.entrySet()) {
            for (DashBoard dashBoard : entry.getValue().profile.dashBoards) {
                for (Widget widget : dashBoard.widgets) {
                    if (widget instanceof Timer) {
                        Timer timer = (Timer) widget;
                        add(entry.getKey(), timer, dashBoard.id);
                        counter++;
                    }
                }
            }
        }
        log.info("Timers : {}", counter);
    }

    public void add(UserKey userKey, Timer timer, int dashId) {
        if (timer.isValidStart()) {
            timerExecutors[hash(timer.startTime)].put(new TimerKey(userKey, dashId, timer, timer.startTime, timer.startValue), EMPTY_VALUE);
        }
        if (timer.isValidStop()) {
            timerExecutors[hash(timer.stopTime)].put(new TimerKey(userKey, dashId, timer, timer.stopTime, timer.stopValue), EMPTY_VALUE);
        }
    }

    public void delete(UserKey userKey, Timer timer, int dashId) {
        if (timer.isValidStart()) {
            timerExecutors[hash(timer.startTime)].remove(new TimerKey(userKey, dashId, timer, timer.startTime, timer.startValue));
        }
        if (timer.isValidStop()) {
            timerExecutors[hash(timer.stopTime)].remove(new TimerKey(userKey, dashId, timer, timer.stopTime, timer.stopValue));
        }
    }

    private int actuallySendTimers;

    @Override
    public void run() {
        log.trace("Starting timer...");

        int curSeconds = LocalTime.now(DateTimeUtils.UTC).toSecondOfDay();
        ConcurrentMap<TimerKey, String> tickedExecutors = timerExecutors[hash(curSeconds)];

        int readyForTickTimers = tickedExecutors.size();
        if (readyForTickTimers == 0) {
            return;
        }

        final long curTime = System.currentTimeMillis();
        int activeTimers = 0;
        actuallySendTimers = 0;

        for (TimerKey key : tickedExecutors.keySet()) {
            if (key.exactlyTime == curSeconds) {
                User user = userDao.users.get(key.userKey);
                if (user != null) {
                    DashBoard dash = user.profile.getDashById(key.dashId);
                    if (dash != null && dash.isActive) {
                        activeTimers++;
                        triggerTimer(sessionDao, key.userKey, key.value, key.dashId, key.timer.deviceId);
                        key.timer.value = key.value;
                    }
                }
            }
        }

        if (activeTimers > 0) {
            log.info("Timer finished. Ready {}, Active {}, Actual {}. Processing time : {} ms",
                    readyForTickTimers, activeTimers, actuallySendTimers, System.currentTimeMillis() - curTime);
        }
    }

    private void triggerTimer(SessionDao sessionDao, UserKey userKey, String value, int dashId, int deviceId) {
        Session session = sessionDao.userSession.get(userKey);
        if (session != null) {
            if (!session.sendMessageToHardware(dashId, HARDWARE, 7777, value, deviceId)) {
                actuallySendTimers++;
            }
        }
    }

}
