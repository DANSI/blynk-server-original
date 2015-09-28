package cc.blynk.server.workers;

import cc.blynk.common.enums.Command;
import cc.blynk.common.stats.GlobalStats;
import cc.blynk.server.dao.SessionDao;
import cc.blynk.server.dao.UserDao;
import cc.blynk.server.model.auth.Session;
import cc.blynk.server.model.auth.User;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Map;
import java.util.concurrent.atomic.LongAdder;

/**
 * Worker responsible for logging current request rate,
 * methods invocation statistic and active channels count.
 *
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 18.04.15.
 */
public class StatsWorker implements Runnable {

    private static final Logger log = LogManager.getLogger(StatsWorker.class);

    private final GlobalStats stats;
    private final SessionDao sessionDao;
    private final UserDao userDao;

    public StatsWorker(GlobalStats stats, SessionDao sessionDao, UserDao userDao) {
        this.stats = stats;
        this.sessionDao = sessionDao;
        this.userDao = userDao;
    }

    @Override
    public void run() {
        Stat stat = new Stat();
        stat.oneMinRate = (long) stats.incomeMessages.getOneMinuteRate();

        //yeap, some stats updates may be lost (because of sumThenReset()),
        //but we don't care, cause this is just for general monitoring
        for (Map.Entry<Short, String> counterEntry : Command.valuesName.entrySet()) {
            LongAdder longAdder = stats.specificCounters[counterEntry.getKey()];
            stat.messages.put(counterEntry.getValue(), longAdder.sumThenReset());
        }

        int activeSessions = 0;
        int hardActive = 0;
        int appActive = 0;
        for (Map.Entry<User, Session> entry: sessionDao.getUserSession().entrySet()) {
            Session session = entry.getValue();
            if (session.hardwareChannels.size() > 0 && session.appChannels.size() > 0) {
                activeSessions++;
            }
            if (session.hardwareChannels.size() > 0) {
                hardActive++;
            }
            if (session.appChannels.size() > 0) {
                appActive++;
            }

        }

        stat.connected = activeSessions;
        stat.onlineApps = appActive;
        stat.onlineHards = hardActive;
        stat.active = sessionDao.getUserSession().size();
        stat.total = userDao.getUsers().size();

        log.info(stat.toString());
    }

}

