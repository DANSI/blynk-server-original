package cc.blynk.server.workers;

import cc.blynk.server.core.dao.SessionDao;
import cc.blynk.server.core.dao.UserDao;
import cc.blynk.server.core.dao.UserKey;
import cc.blynk.server.core.model.DashBoard;
import cc.blynk.server.core.model.auth.Session;
import cc.blynk.server.core.model.auth.User;
import cc.blynk.server.core.model.widgets.FrequencyWidget;
import cc.blynk.server.core.model.widgets.Widget;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Map;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 02.02.17.
 */
public class ReadingWidgetsWorker implements Runnable {

    private static final Logger log = LogManager.getLogger(ReadingWidgetsWorker.class);

    private final SessionDao sessionDao;
    private final UserDao userDao;

    public ReadingWidgetsWorker(SessionDao sessionDao, UserDao userDao) {
        this.sessionDao = sessionDao;
        this.userDao = userDao;
    }

    @Override
    public void run() {
        log.debug("Starting reading widget worker.");
        long now = System.currentTimeMillis();
        int tickedWidgets = 0;
        for (Map.Entry<UserKey, Session> entry : sessionDao.userSession.entrySet()) {
            final Session session = entry.getValue();
            //for now checking widgets for active app only
            if (session.isAppConnected() && session.isHardwareConnected()) {
                final UserKey userKey = entry.getKey();
                final User user = userDao.users.get(userKey);
                for (DashBoard dashBoard : user.profile.dashBoards) {
                    if (dashBoard.isActive) {
                        //todo improve for better performance
                        //for now this is just quick implementation
                        for (Widget widget : dashBoard.widgets) {
                            if (widget instanceof FrequencyWidget) {
                                FrequencyWidget frequencyWidget = (FrequencyWidget) widget;
                                if (frequencyWidget.isTicked(now)) {
                                    tickedWidgets++;
                                    frequencyWidget.sendReadingCommand(session, dashBoard.id);
                                }
                            }
                        }
                    }
                }
            }
        }
        log.debug("Starting reading widget worker. Ticket widgets : {}. Time : {}", tickedWidgets, System.currentTimeMillis() - now);
    }

}
