package cc.blynk.server.workers;

import cc.blynk.common.stats.GlobalStats;
import cc.blynk.server.dao.SessionsHolder;
import cc.blynk.server.dao.UserRegistry;
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
    private final UserRegistry userRegistry;

    public StatsWorker(GlobalStats stats, SessionsHolder sessionsHolder, UserRegistry userRegistry) {
        this.stats = stats;
        this.sessionsHolder = sessionsHolder;
        this.userRegistry = userRegistry;
    }

    @Override
    public void run() {
        Stat stat = new Stat();
        stat.oneMinRate = (long) stats.incomeMessages.getOneMinuteRate();

        //yeap, some stats updates may be lost (because if sumThenReset()),
        //but I don't care, cause this is just for general monitoring
        for (Map.Entry<Class<?>, LongAdder> counterEntry : stats.specificCounters.entrySet()) {
            stat.messages.put(counterEntry.getKey().getSimpleName(), counterEntry.getValue().sumThenReset());
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

        stat.connected = activeSessions;
        stat.onlineApps = appActive;
        stat.onlineHards = hardActive;
        stat.active = sessionsHolder.getUserSession().size();
        stat.total = userRegistry.getUsers().size();

        log.info(stat.toString());
    }

}

