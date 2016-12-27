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
            ConcurrentMap<TimerKey, String> timers = getOrCreateIfEmpty(timer.startTime);
            timers.put(new TimerKey(userKey, timer, dashId, START), timer.startValue);
        }
        if (timer.isValidStop()) {
            ConcurrentMap<TimerKey, String> timers = getOrCreateIfEmpty(timer.stopTime);
            timers.put(new TimerKey(userKey, timer, dashId, STOP), timer.stopValue);
        }
    }

    public void delete(UserKey userKey, Timer timer, int dashId) {
        if (timer.isValidStart()) {
            ConcurrentMap<TimerKey, String> timers = getOrCreateIfEmpty(timer.startTime);
            timers.remove(new TimerKey(userKey, timer, dashId, START));
        }
        if (timer.isValidStop()) {
            ConcurrentMap<TimerKey, String> timers = getOrCreateIfEmpty(timer.stopTime);
            timers.remove(new TimerKey(userKey, timer, dashId, STOP));
        }
    }


    /**
     * Get array cell or fills it with ConcurrentHashMap if was empty before.
     *
     * @param index - array cell index
     * @return return cell ConcurrentMap
     */
    private ConcurrentMap<TimerKey, String> getOrCreateIfEmpty(int index) {
        ConcurrentMap<TimerKey, String> timers = timerExecutors.get(index);

        //Here is small chance that few threads will access this method at same time.
        //So doing it threadsafe.
        if (timers == null) {
            ConcurrentMap<TimerKey, String>  update = new ConcurrentHashMap<>();
            if (timerExecutors.compareAndSet(index, null, update)) {
                return update;
            }
            return timerExecutors.get(index);
        }

        return timers;
    }

    private int actuallySendTimers;

    @Override
    public void run() {
        log.trace("Starting timer...");

        int curSeconds = LocalTime.now(DateTimeUtils.UTC).toSecondOfDay();
        ConcurrentMap<TimerKey, String> tickedExecutors = timerExecutors.get(curSeconds);

        int readyForTickTimers;
        if (tickedExecutors == null || ((readyForTickTimers = tickedExecutors.size()) == 0)) {
            return;
        }

        final long curTime = System.currentTimeMillis();
        int activeTimers = 0;
        actuallySendTimers = 0;

        for (Map.Entry<TimerKey, String> entry : tickedExecutors.entrySet()) {
            final TimerKey key = entry.getKey();
            User user = userDao.users.get(key.userKey);
            if (user != null) {
                DashBoard dash = user.profile.getDashById(key.dashId);
                if (dash != null && dash.isActive) {
                    activeTimers++;
                    final String value = entry.getValue();
                    triggerTimer(sessionDao, key.userKey, value, key.dashId, key.timer.deviceId);
                    key.timer.value = value;
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
