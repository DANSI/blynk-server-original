package cc.blynk.server.core.dao;

import cc.blynk.server.core.model.auth.User;

import java.util.Collection;
import java.util.concurrent.ConcurrentMap;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 14.10.16.
 */
public class TokenManager {

    public final TokenManagerBase regularTokenManager;
    public final TokenManagerBase sharedTokenManager;

    public TokenManager(ConcurrentMap<UserKey, User> users) {
        Collection<User> allUsers = users.values();
        this.regularTokenManager = new RegularTokenManager(allUsers);
        this.sharedTokenManager = new SharedTokenManager(allUsers);
    }

    public void deleteProject(User user, Integer projectId) {
        regularTokenManager.deleteProject(user, projectId);
        sharedTokenManager.deleteProject(user, projectId);
    }
}
