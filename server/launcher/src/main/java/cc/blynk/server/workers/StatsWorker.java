package cc.blynk.server.workers;

import cc.blynk.common.stats.GlobalStats;
import cc.blynk.server.dao.SessionDao;
import cc.blynk.server.dao.UserDao;
import cc.blynk.server.stats.Stat;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

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
        Stat stat = Stat.calcStats(sessionDao, userDao, stats, true);
        log.info(stat.toJson());
    }

}

