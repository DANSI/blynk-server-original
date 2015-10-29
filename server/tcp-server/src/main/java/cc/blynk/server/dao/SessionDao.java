package cc.blynk.server.dao;

import cc.blynk.server.handlers.app.main.auth.AppStateHolder;
import cc.blynk.server.handlers.hardware.auth.HardwareStateHolder;
import cc.blynk.server.model.auth.Session;
import cc.blynk.server.model.auth.User;
import io.netty.channel.Channel;
import io.netty.channel.EventLoop;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static cc.blynk.server.utils.StateHolderUtil.*;

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

    public void removeAppFromSession(Channel channel) {
        AppStateHolder state = getAppState(channel);
        if (state == null) {
            state = getShareState(channel);
        }
        if (state != null) {
            Session session = userSession.get(state.user);
            if (session != null) {
                session.appChannels.remove(channel);
            }
        }
    }

    public void removeHardFromSession(Channel channel) {
        HardwareStateHolder state = getHardState(channel);
        if (state != null) {
            Session session = userSession.get(state.user);
            if (session != null) {
                session.hardwareChannels.remove(channel);
            }
        }
    }

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