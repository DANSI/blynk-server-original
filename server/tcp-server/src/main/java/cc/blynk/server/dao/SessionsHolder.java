package cc.blynk.server.dao;

import cc.blynk.server.model.auth.ChannelState;
import cc.blynk.server.model.auth.Session;
import cc.blynk.server.model.auth.User;
import io.netty.channel.Channel;
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
public class SessionsHolder {

    private static final Logger log = LogManager.getLogger(SessionsHolder.class);

    public final Map<User, Session> userSession = new ConcurrentHashMap<>();

    public void addChannelToGroup(User user, Channel channel, int msgId) {
        Session session = getSessionByUser(user);
        session.addChannel(channel, msgId);
    }

    public void removeFromSession(Channel channel) {
        User user = channel.attr(ChannelState.USER).get();
        if (user != null) {
            Session session = userSession.get(user);
            if (session != null) {
                session.remove(channel);
            }
        }
    }

    //threadsafe
    private Session getSessionByUser(User user) {
        Session group = userSession.get(user);
        //only one side came
        if (group == null) {
            Session value = new Session();
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