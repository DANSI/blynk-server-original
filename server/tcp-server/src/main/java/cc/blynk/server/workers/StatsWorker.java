package cc.blynk.server.workers;

import cc.blynk.common.stats.GlobalStats;
import cc.blynk.server.dao.SessionsHolder;
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
    private final SessionsHolder sessionsHolder;

    public StatsWorker(GlobalStats stats, SessionsHolder sessionsHolder) {
        this.stats = stats;
        this.sessionsHolder = sessionsHolder;
    }

    @Override
    public void run() {
        //do not log low traffic. it is not interesting =).
        if (stats.incomeMessages.getOneMinuteRate() > 1) {
            log.info("1 min rate : {}", String.format("%.2f", stats.incomeMessages.getOneMinuteRate()));
            for (Map.Entry<Class<?>, LongAdder> counterEntry : stats.specificCounters.entrySet()) {
                log.info("{} : {}", counterEntry.getKey().getSimpleName(), counterEntry.getValue().sum());
            }
            log.info("--------------------------------------------------------------------------------------");
        }

        int activeSessions = 0;
        int hardActive = 0;
        int appActive = 0;
        for (Map.Entry<User, Session> entry: sessionsHolder.getUserSession().entrySet()) {
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

        if (appActive > 0 || hardActive > 0) {
            log.info("Total {}, active {}, apps {}, hards {}",
                    sessionsHolder.getUserSession().size(), activeSessions, appActive, hardActive);
        }
    }
}
