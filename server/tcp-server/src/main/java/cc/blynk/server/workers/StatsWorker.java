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
    private final static long ONE_DAY = 24 * 60 * 60 * 1000;
    private final static long THREE_DAYS = 3 * 24 * 60 * 60 * 1000;
    private final GlobalStats stats;
    private final SessionDao sessionDao;
    private final UserDao userDao;

    public StatsWorker(GlobalStats stats, SessionDao sessionDao, UserDao userDao) {
        this.stats = stats;
        this.sessionDao = sessionDao;
        this.userDao = userDao;
    }

    public static Stat calcStats(SessionDao sessionDao, UserDao userDao, GlobalStats localStats, boolean reset) {
        Stat stat = new Stat();
        stat.oneMinRate = (long) localStats.incomeMessages.getOneMinuteRate();

        //yeap, some stats updates may be lost (because of sumThenReset()),
        //but we don't care, cause this is just for general monitoring
        for (Map.Entry<Short, String> counterEntry : Command.valuesName.entrySet()) {
            LongAdder longAdder = localStats.specificCounters[counterEntry.getKey()];
            stat.messages.put(counterEntry.getValue(), reset ? longAdder.sumThenReset() : longAdder.sum());
        }

        int connectedSessions = 0;
        int hardActive = 0;
        int appActive = 0;
        int active = 0;
        int active3 = 0;
        long now = System.currentTimeMillis();
        for (Map.Entry<User, Session> entry: sessionDao.userSession.entrySet()) {
            Session session = entry.getValue();

            if (session.hardwareChannels.size() > 0 && session.appChannels.size() > 0) {
                connectedSessions++;
            }
            if (session.hardwareChannels.size() > 0) {
                hardActive++;
            }
            if (session.appChannels.size() > 0) {
                appActive++;
            }
            if (now - entry.getKey().lastModifiedTs < ONE_DAY) {
                active++;
            }
            if (now - entry.getKey().lastModifiedTs < THREE_DAYS) {
                active3++;
            }

        }

        stat.connected = connectedSessions;
        stat.onlineApps = appActive;
        stat.onlineHards = hardActive;
        stat.active = active;
        stat.active3 = active3;
        stat.total = userDao.getUsers().size();

        return stat;
    }

    @Override
    public void run() {
        Stat stat = calcStats(sessionDao, userDao, stats, true);
        log.info(stat.toString());
    }

}

