package cc.blynk.server.core.dao;

import cc.blynk.server.core.model.auth.Session;
import cc.blynk.server.core.model.auth.User;
import io.netty.channel.EventLoop;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Holds session info related to specific user.
 *
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 2/18/2015.
 */
public class SessionDao {

    private static final Logger log = LogManager.getLogger(SessionDao.class);

    public final Map<User, Session> userSession = new ConcurrentHashMap<>();

    //threadsafe
    public Session getSessionByUser(User user, EventLoop initialEventLoop) {
        Session group = userSession.get(user);
        //only one side came
        if (group == null) {
            Session value = new Session(initialEventLoop);
            group = userSession.putIfAbsent(user, value);
            if (group == null) {
                log.trace("Creating unique session for user: {}", user);
                return value;
            }
        }

        return group;
    }

    //for test only
    public Map<User, Session> getUserSession() {
        return userSession;
    }
}