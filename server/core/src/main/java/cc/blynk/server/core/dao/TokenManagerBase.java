package cc.blynk.server.core.dao;

import cc.blynk.server.core.model.auth.User;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 22.09.15.
 */
abstract class TokenManagerBase {

    protected final ConcurrentMap<String, TokenValue> cache;

    public TokenManagerBase(ConcurrentMap<String, TokenValue> data) {
        this.cache = data;
    }

    protected static void cleanTokensForNonExistentDashes(User user, Map<Integer, String> tokens) {
        Iterator<Integer> iterator = tokens.keySet().iterator();
        while (iterator.hasNext()) {
            if (!user.dashIdExists(iterator.next())) {
                iterator.remove();
            }
        }
    }

    public TokenValue getUserByToken(String token) {
        return cache.get(token);
    }

    abstract String deleteProject(User user, Integer projectId);

    abstract void printMessage(String username, int dashId, String token);

}
