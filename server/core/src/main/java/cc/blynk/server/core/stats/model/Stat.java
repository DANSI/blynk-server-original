package cc.blynk.server.core.stats.model;

import cc.blynk.server.core.dao.SessionDao;
import cc.blynk.server.core.dao.UserDao;
import cc.blynk.server.core.dao.UserKey;
import cc.blynk.server.core.model.auth.Session;
import cc.blynk.server.core.model.auth.User;
import cc.blynk.server.core.protocol.enums.Command;
import cc.blynk.server.core.stats.GlobalStats;
import cc.blynk.utils.JsonParser;

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

    public final CommandStat commands = new CommandStat();
    public final HttpStat http = new HttpStat();

    long oneMinRate;
    long total;
    long active;
    long active3;
    long connected;
    long onlineApps;
    long totalOnlineApps;
    long onlineHards;
    long totalOnlineHards;

    public Stat(SessionDao sessionDao, UserDao userDao, GlobalStats localStats, boolean reset) {
        //yeap, some stats updates may be lost (because of sumThenReset()),
        //but we don't care, cause this is just for general monitoring
        for (Short command : Command.valuesName.keySet()) {
            LongAdder longAdder = localStats.specificCounters[command];
            long val = reset ? longAdder.sumThenReset() : longAdder.sum();

            this.http.assign(command, val);
            this.commands.assign(command, val);
        }

        this.oneMinRate = (long) localStats.totalMessages.getOneMinuteRate();
        int connectedSessions = 0;

        int hardActive = 0;
        int totalOnlineHards = 0;

        int appActive = 0;
        int totalOnlineApps = 0;

        int active = 0;
        int active3 = 0;

        long now = System.currentTimeMillis();
        for (Map.Entry<UserKey, Session> entry: sessionDao.userSession.entrySet()) {
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
            UserKey userKey = entry.getKey();
            User user = userDao.users.get(userKey);

            if (user != null) {
                if (now - user.lastModifiedTs < ONE_DAY) {
                    active++;
                }
                if (now - user.lastModifiedTs < THREE_DAYS) {
                    active3++;
                }
            }
        }

        this.connected = connectedSessions;
        this.onlineApps = appActive;
        this.totalOnlineApps = totalOnlineApps;
        this.onlineHards = hardActive;
        this.totalOnlineHards = totalOnlineHards;

        this.active = active;
        this.active3 = active3;
        this.total = userDao.users.size();
    }

    public String toJson() {
        return JsonParser.toJson(this);
    }
}
