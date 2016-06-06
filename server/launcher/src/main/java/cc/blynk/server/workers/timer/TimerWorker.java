package cc.blynk.server.workers.timer;

import cc.blynk.server.core.dao.SessionDao;
import cc.blynk.server.core.dao.UserDao;
import cc.blynk.server.core.model.DashBoard;
import cc.blynk.server.core.model.auth.Session;
import cc.blynk.server.core.model.auth.User;
import cc.blynk.server.core.model.widgets.Widget;
import cc.blynk.server.core.model.widgets.controls.Timer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.LocalTime;
import java.time.ZoneId;

import static cc.blynk.server.core.protocol.enums.Command.*;

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
    private final ZoneId UTC = ZoneId.of("UTC");

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

        LocalTime localTime = LocalTime.now(UTC);

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
        if (sendMessageIfTicked(user, curSeconds, timer.startTime, timer.startValue, dashId)) {
            timer.value = timer.startValue;
        }
        if (sendMessageIfTicked(user, curSeconds, timer.stopTime, timer.stopValue, dashId)) {
            timer.value = timer.stopValue;
        }
    }

    //todo simplify, move "if" to separate method
    private boolean sendMessageIfTicked(User user, long curSeconds, long time, String value, int dashId) {
        if (time != -1 && value != null && !value.equals("") && curSeconds == time) {
            tickedTimers++;
            Session session = sessionDao.userSession.get(user);
            if (session != null) {
                onlineTimers++;
                if (session.getHardwareChannels().size() > 0) {
                    session.sendMessageToHardware(dashId, HARDWARE, 7777, value);
                }
            }
            return true;
        }
        return false;
    }

}
