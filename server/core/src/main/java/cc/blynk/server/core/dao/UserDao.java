package cc.blynk.server.core.dao;

import cc.blynk.server.core.model.DashBoard;
import cc.blynk.server.core.model.auth.User;
import cc.blynk.server.core.model.widgets.Widget;
import cc.blynk.server.core.protocol.exceptions.InvalidTokenException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

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
        throw new InvalidTokenException("Error getting dashId. Wrong token.", msgId);
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

    public List<User> searchByUsername(String name) {
        if (name == null) {
            return new ArrayList<>(users.values());
        }

        return users.values().stream().filter(user -> user.name.contains(name)).collect(Collectors.toList());
    }

    public void deleteProject(User user, Integer projectId) {
        tokenManager.deleteProject(user, projectId);
        sharedTokenManager.deleteProject(user, projectId);
    }

    public User delete(String name) {
        return users.remove(name);
    }

    public User add(User user) {
        return users.put(user.name, user);
    }

    public Map<String, Integer> getBoardsUsage() {
        Map<String, Integer> boards = new HashMap<>();
        for (User user : users.values()) {
            for (DashBoard dashBoard : user.profile.dashBoards) {
                String type = dashBoard.boardType == null ? "Not Selected" : dashBoard.boardType;
                Integer i = boards.getOrDefault(type, 0);
                boards.put(type, ++i);
            }
        }
        return boards;
    }

    public Map<String, Integer> getWidgetsUsage() {
        Map<String, Integer> widgets = new HashMap<>();
        for (User user : users.values()) {
            for (DashBoard dashBoard : user.profile.dashBoards) {
                if (dashBoard.widgets != null) {
                    for (Widget widget : dashBoard.widgets) {
                        Integer i = widgets.getOrDefault(widget.getClass().getSimpleName(), 0);
                        widgets.put(widget.getClass().getSimpleName(), ++i);
                    }
                }
            }
        }
        return widgets;
    }

    public Map<String, Integer> getProjectsPerUser() {
        Map<String, Integer> projectsPerUser = new HashMap<>();
        for (User user : users.values()) {
            String key = String.valueOf(user.profile.dashBoards.length);
            Integer i = projectsPerUser.getOrDefault(key, 0);
            projectsPerUser.put(key, ++i);
        }
        return projectsPerUser;
    }

    public Map<String, Integer> getFilledSpace() {
        Map<String, Integer> filledSpace = new HashMap<>();
        for (User user : users.values()) {
            for (DashBoard dashBoard : user.profile.dashBoards) {
                int sum = 0;
                for (Widget widget : dashBoard.widgets) {
                    if (widget.height < 0 || widget.width < 0) {
                        //log.error("Widget without length fields. User : {}", user.name);
                        continue;
                    }
                    sum += widget.height * widget.width;
                }
                if (sum > 74) {
                    log.error("Error in profile, too many widgets. User : {}. DashId : {}", user.name, dashBoard.id);
                }

                String key = String.valueOf(sum);
                Integer i = filledSpace.getOrDefault(key, 0);
                filledSpace.put(key, ++i);
            }
        }
        return filledSpace;
    }

    public User addFacebookUser(String userName) {
        log.debug("Adding new facebook user {}.", userName);
        User newUser = new User(userName);
        users.put(userName, newUser);
        return newUser;
    }

    public void add(String userName, String pass) {
        User newUser = new User(userName, pass);
        users.put(userName, newUser);
    }

}
