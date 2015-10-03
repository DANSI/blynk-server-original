package cc.blynk.server.dao;

import cc.blynk.server.exceptions.InvalidTokenException;
import cc.blynk.server.model.auth.User;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Map;
import java.util.concurrent.ConcurrentMap;

/**
 * Helper class for holding info regarding registered users and profiles.
 *
 * User: ddumanskiy
 * Date: 8/11/13
 * Time: 4:02 PM
 */
public class UserDao {

    private static final Logger log = LogManager.getLogger(UserDao.class);
    public final TokenManagerBase tokenManager;
    public final TokenManagerBase sharedTokenManager;
    private final ConcurrentMap<String, User> users;

    public UserDao(ConcurrentMap<String, User> users) {
        //reading DB to RAM.
        this.users = users;
        this.tokenManager = new TokenManager(users.values());
        this.sharedTokenManager = new SharedTokenManager(users.values());
    }

    public static Integer getDashIdByToken(Map<Integer, String> tokens, String token, int msgId) {
        for (Map.Entry<Integer, String> dashToken : tokens.entrySet()) {
            if (dashToken.getValue().equals(token)) {
                return dashToken.getKey();
            }
        }
        throw new InvalidTokenException("Error getting dashId.", msgId);
    }

    public boolean isUserExists(String name) {
        return users.get(name) != null;
    }

    public User getByName(String name) {
        return users.get(name);
    }

    public Map<String, User> getUsers() {
        return users;
    }

    public void createNewUser(String userName, String pass) {
        User newUser = new User(userName, pass);
        users.put(userName, newUser);
    }



}
