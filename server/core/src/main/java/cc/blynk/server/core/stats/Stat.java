package cc.blynk.server.core.stats;

import cc.blynk.server.core.dao.SessionDao;
import cc.blynk.server.core.dao.UserDao;
import cc.blynk.server.core.model.auth.Session;
import cc.blynk.server.core.model.auth.User;
import cc.blynk.server.core.protocol.enums.Command;
import cc.blynk.utils.JsonParser;
import com.fasterxml.jackson.databind.ObjectWriter;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.LongAdder;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 19.07.15.
 */
public class Stat {

    private final static long ONE_DAY = 24 * 60 * 60 * 1000;
    private final static long THREE_DAYS = 3 * ONE_DAY;

    private static final ObjectWriter statWriter = JsonParser.init().writerWithDefaultPrettyPrinter().forType(Stat.class);

    public final Map<String, Long> messages = new HashMap<>();
    public final Map<String, Long> http = new HashMap<>();

    long oneMinRate;
    long total;
    long active;
    long active3;
    long connected;
    long onlineApps;
    long totalOnlineApps;
    long onlineHards;
    long totalOnlineHards;

    public static Stat calcStats(SessionDao sessionDao, UserDao userDao, GlobalStats localStats, boolean reset) {
        Stat stat = new Stat();
        stat.oneMinRate = (long) localStats.incomeMessages.getOneMinuteRate();

        //yeap, some stats updates may be lost (because of sumThenReset()),
        //but we don't care, cause this is just for general monitoring
        for (Map.Entry<Short, String> counterEntry : Command.valuesName.entrySet()) {
            LongAdder longAdder = localStats.specificCounters[counterEntry.getKey()];
            String key = counterEntry.getValue();
            if (key.startsWith("Http")) {
                stat.http.put(key, reset ? longAdder.sumThenReset() : longAdder.sum());
            } else {
                stat.messages.put(key, reset ? longAdder.sumThenReset() : longAdder.sum());
            }
        }

        int connectedSessions = 0;

        int hardActive = 0;
        int totalOnlineHards = 0;

        int appActive = 0;
        int totalOnlineApps = 0;

        int active = 0;
        int active3 = 0;

        long now = System.currentTimeMillis();
        for (Map.Entry<User, Session> entry: sessionDao.userSession.entrySet()) {
            Session session = entry.getValue();

            if (session.getHardwareChannels().size() > 0 && session.getAppChannels().size() > 0) {
                connectedSessions++;
            }
            if (session.getHardwareChannels().size() > 0) {
                hardActive++;
                totalOnlineHards += session.getHardwareChannels().size();
            }
            if (session.getAppChannels().size() > 0) {
                appActive++;
                totalOnlineApps += session.getAppChannels().size();
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
        stat.totalOnlineApps = totalOnlineApps;
        stat.onlineHards = hardActive;
        stat.totalOnlineHards = totalOnlineHards;

        stat.active = active;
        stat.active3 = active3;
        stat.total = userDao.getUsers().size();

        return stat;
    }

    public String toJson() {
        return JsonParser.toJson(statWriter, this);
    }
}
