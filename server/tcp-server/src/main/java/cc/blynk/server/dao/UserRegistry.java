package cc.blynk.server.dao;

import cc.blynk.server.model.auth.User;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Helper class for holding info regarding registered users and profiles.
 *
 * User: ddumanskiy
 * Date: 8/11/13
 * Time: 4:02 PM
 */
public class UserRegistry {

    private static final Logger log = LogManager.getLogger(UserRegistry.class);
    private final Map<String, User> users;
    private final Map<String, User> tokenToUserCache;
    //init user DB if possible

    public UserRegistry(Map<String, User> users) {
        //reading DB to RAM.
        this.users = users;
        tokenToUserCache = createTokenToUserCache(users);
    }

    public UserRegistry(Map<String, User> users, Map<String, User> usersFromAnotherSource) {
        this.users = users;
        this.users.putAll(usersFromAnotherSource);
        tokenToUserCache = createTokenToUserCache(users);
    }

    public static Integer getDashIdByToken(User user, String token) {
        for (Map.Entry<Integer, String> dashToken : user.getDashTokens().entrySet()) {
            if (dashToken.getValue().equals(token)) {
                return dashToken.getKey();
            }
        }
        throw new RuntimeException("Error getting dashId for user. FIX/");
    }

    private static String generateNewToken() {
        return UUID.randomUUID().toString().replace("-", "");
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

    public User getUserByToken(String token) {
        return tokenToUserCache.get(token);
    }

    public String getToken(User user, Integer dashboardId) {
        Map<Integer, String> dashTokens = user.getDashTokens();
        String token = dashTokens.get(dashboardId);

        //if token not exists. generate new one
        if (token == null) {
            log.info("Token for user {} and dashId {} not generated yet.", user.getName(), dashboardId);
            token = refreshToken(user, dashboardId);
        } else {
            log.info("Token for user {} and dashId {} generated already. Token {}", user.getName(), dashboardId, token);
        }

        return token;
    }

    public String refreshToken(User user, Integer dashboardId) {
        // Clean old token from cache if exists.
        String oldToken = user.getDashTokens().get(dashboardId);
        if (oldToken != null) tokenToUserCache.remove(oldToken);

        //Create new token
        String newToken = generateNewToken();
        user.putToken(dashboardId, newToken);
        tokenToUserCache.put(newToken, user);

        log.info("Generated newToken for user {} and dashId {} is {}.", user.getName(), dashboardId, newToken);
        return newToken;
    }

    public void createNewUser(String userName, String pass) {
        User newUser = new User(userName, pass);
        users.put(userName, newUser);
    }

    private Map<String, User> createTokenToUserCache(final Map<String, User> users) {
        return new HashMap<String, User>() {{
            for (User user : users.values()) {
                for (String userToken : user.getDashTokens().values()) {
                    put(userToken, user);
                }
            }
        }};
    }

}
