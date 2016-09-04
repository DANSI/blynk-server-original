package cc.blynk.server.workers.timer;

import cc.blynk.server.core.dao.SessionDao;
import cc.blynk.server.core.dao.UserDao;
import cc.blynk.server.core.model.DashBoard;
import cc.blynk.server.core.model.auth.Session;
import cc.blynk.server.core.model.auth.User;
import cc.blynk.server.core.model.widgets.Widget;
import cc.blynk.server.core.model.widgets.controls.Timer;
import cc.blynk.utils.DateTimeUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.LocalTime;

import static cc.blynk.server.core.protocol.enums.Command.HARDWARE;

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

    private int tickedTimers;
    private int onlineTimers;

    public TimerWorker(UserDao userDao, SessionDao sessionDao) {
        this.userDao = userDao;
        this.sessionDao = sessionDao;
    }

    @Override
    public void run() {
        log.trace("Starting timer...");
        tickedTimers = 0;
        onlineTimers = 0;

        LocalTime localTime = LocalTime.now(DateTimeUtils.UTC);

        long curSeconds = localTime.getSecond() + localTime.getMinute() * 60 + localTime.getHour() * 3600;
        checkTimers(curSeconds);

        //logging only events when timers ticked.
        if (onlineTimers > 0) {
            log.info("Timer finished. Processed {}/{} timers.", onlineTimers, tickedTimers);
        }
    }

    protected void checkTimers(long curSeconds) {
        for (User user : userDao.getUsers().values()) {
            for (DashBoard dashBoard : user.profile.dashBoards) {
                if (dashBoard.isActive) {
                    for (Widget widget : dashBoard.widgets) {
                        if (widget instanceof Timer) {
                            Timer timer = (Timer) widget;
                            send(user, timer, curSeconds, dashBoard.id);
                        }
                    }
                }
            }
        }
    }

    private void send(User user, Timer timer, long curSeconds, int dashId) {
        if (isTicked(curSeconds, timer.startTime, timer.startValue)) {
            triggerTimer(user, timer.startValue, dashId);
            timer.value = timer.startValue;
        }

        if (isTicked(curSeconds, timer.stopTime, timer.stopValue)) {
            triggerTimer(user, timer.stopValue, dashId);
            timer.value = timer.stopValue;
        }
    }

    private void triggerTimer(User user, String value, int dashId) {
        tickedTimers++;
        Session session = sessionDao.userSession.get(user);
        if (session != null) {
            onlineTimers++;
            if (session.getHardwareChannels().size() > 0) {
                session.sendMessageToHardware(dashId, HARDWARE, 7777, value);
            }
        }
    }

    private static boolean isTicked(long curSeconds, long startTime, String value) {
        return curSeconds == startTime && startTime != -1 && value != null && !value.equals("");
    }

}
