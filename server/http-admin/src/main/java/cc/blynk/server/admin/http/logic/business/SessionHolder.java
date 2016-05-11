package cc.blynk.server.admin.http.logic.business;

import cc.blynk.server.core.model.auth.User;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 12.05.16.
 */
public class SessionHolder {

    private final ConcurrentMap<String, User> httpSession = new ConcurrentHashMap<>();

    public String generateNewSession(User user) {
        String sessionId = UUID.randomUUID().toString();
        httpSession.put(sessionId, user);
        return sessionId;
    }

}
