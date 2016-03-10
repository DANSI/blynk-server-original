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

        LocalTime localDateTime = LocalTime.now(UTC);

        long curTime = localDateTime.getSecond() + localDateTime.getMinute() * 60 + localDateTime.getHour() * 3600;

        for (User user : userDao.getUsers().values()) {
            for (DashBoard dashBoard : user.profile.dashBoards) {
                if (dashBoard.isActive) {
                    for (Widget widget : dashBoard.widgets) {
                        if (widget instanceof Timer) {
                            Timer timer = (Timer) widget;
                            send(user, timer, curTime, dashBoard.id);
                        }
                    }
                }
            }
        }

        //logging only events when timers ticked.
        if (onlineTimers > 0) {
            log.info("Timer finished. Processed {}/{} timers.", onlineTimers, tickedTimers);
        }
    }

    private void send(User user, Timer timer, long curTime, int dashId) {
        sendMessageIfTicked(user, curTime, timer.startTime, timer.startValue, dashId);
        sendMessageIfTicked(user, curTime, timer.stopTime, timer.stopValue, dashId);
    }

    private void sendMessageIfTicked(User user, long curTime, long time, String value, int dashId) {
        if (time != -1 && value != null && !value.equals("") && curTime == time) {
            tickedTimers++;
            Session session = sessionDao.userSession.get(user);
            if (session != null) {
                onlineTimers++;
                if (session.getHardwareChannels().size() > 0) {
                    session.sendMessageToHardware(dashId, 7777, HARDWARE, value);
                }
            }
        }
    }

}
