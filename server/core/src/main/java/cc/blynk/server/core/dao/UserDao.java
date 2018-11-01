package cc.blynk.server.core.dao;

import cc.blynk.server.core.model.DashBoard;
import cc.blynk.server.core.model.auth.App;
import cc.blynk.server.core.model.auth.User;
import cc.blynk.server.core.model.device.Device;
import cc.blynk.server.core.model.enums.ProvisionType;
import cc.blynk.server.core.model.serialization.JsonParser;
import cc.blynk.server.core.model.widgets.Widget;
import cc.blynk.server.core.model.widgets.others.webhook.WebHook;
import cc.blynk.server.workers.timer.TimerWorker;
import cc.blynk.utils.AppNameUtil;
import cc.blynk.utils.TokenGeneratorUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.Arrays;
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

    public final ConcurrentMap<UserKey, User> users;
    private final String region;
    private final String host;

    public UserDao(ConcurrentMap<UserKey, User> users, String region, String host) {
        //reading DB to RAM.
        this.users = users;
        this.region = region;
        this.host = host;
        log.info("Region : {}. Host : {}.", region, host);
    }

    public boolean isUserExists(String name, String appName) {
        return users.get(new UserKey(name, appName)) != null;
    }

    public boolean isSuperAdminExists() {
        User user = getSuperAdmin();
        return user != null;
    }

    public User getSuperAdmin() {
        for (User user : users.values()) {
            if (user.isSuperAdmin) {
                return user;
            }
        }
        return null;
    }

    public User getByName(String name, String appName) {
        return users.get(new UserKey(name, appName));
    }

    public boolean contains(String name, String appName) {
        return users.containsKey(new UserKey(name, appName));
    }

    //for tests only
    public Map<UserKey, User> getUsers() {
        return users;
    }

    public List<User> searchByUsername(String name, String appName) {
        if (name == null) {
            return new ArrayList<>(users.values());
        }

        return users.values().stream().filter(user -> user.email.contains(name)
                && (appName == null || user.appName.equals(appName))).collect(Collectors.toList());
    }

    public User delete(UserKey userKey) {
        return users.remove(userKey);
    }

    public User delete(String name, String appName) {
        return delete(new UserKey(name, appName));
    }

    public void add(User user) {
        users.put(new UserKey(user), user);
    }

    public Map<String, Integer> getBoardsUsage() {
        Map<String, Integer> boards = new HashMap<>();
        for (User user : users.values()) {
            for (DashBoard dashBoard : user.profile.dashBoards) {
                for (Device device : dashBoard.devices) {
                    if (device.boardType != null) {
                        String label = device.boardType.label;
                        Integer i = boards.getOrDefault(label, 0);
                        boards.put(label, ++i);
                    }
                }
            }
        }
        return boards;
    }

    public Map<String, Integer> getFacebookLogin() {
        Map<String, Integer> facebookLogin = new HashMap<>();
        for (User user : users.values()) {
            facebookLogin.compute(
                    user.isFacebookUser
                            ? AppNameUtil.FACEBOOK
                            : AppNameUtil.BLYNK, (k, v) -> v == null ? 1 : v++
            );
        }
        return facebookLogin;
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

    public Map<String, Integer> getLibraryVersion() {
        Map<String, Integer> data = new HashMap<>();
        for (User user : users.values()) {
            for (DashBoard dashBoard : user.profile.dashBoards) {
                for (Device device : dashBoard.devices) {
                    if (device.hardwareInfo != null && device.hardwareInfo.blynkVersion != null) {
                        String key = device.hardwareInfo.blynkVersion;
                        Integer i = data.getOrDefault(key, 0);
                        data.put(key, ++i);
                    }
                }
            }
        }
        return data;
    }

    public Map<String, Integer> getCpuType() {
        Map<String, Integer> data = new HashMap<>();
        for (User user : users.values()) {
            for (DashBoard dashBoard : user.profile.dashBoards) {
                for (Device device : dashBoard.devices) {
                    if (device.hardwareInfo != null && device.hardwareInfo.cpuType != null) {
                        String key = device.hardwareInfo.cpuType;
                        Integer i = data.getOrDefault(key, 0);
                        data.put(key, ++i);
                    }
                }
            }
        }
        return data;
    }

    public Map<String, Integer> getConnectionType() {
        Map<String, Integer> data = new HashMap<>();
        for (User user : users.values()) {
            for (DashBoard dashBoard : user.profile.dashBoards) {
                for (Device device : dashBoard.devices) {
                    if (device.hardwareInfo != null && device.hardwareInfo.connectionType != null) {
                        String key = device.hardwareInfo.connectionType;
                        Integer i = data.getOrDefault(key, 0);
                        data.put(key, ++i);
                    }
                }
            }
        }
        return data;
    }

    public Map<String, Integer> getHardwareBoards() {
        Map<String, Integer> data = new HashMap<>();
        for (User user : users.values()) {
            for (DashBoard dashBoard : user.profile.dashBoards) {
                for (Device device : dashBoard.devices) {
                    if (device.hardwareInfo != null && device.hardwareInfo.boardType != null) {
                        String key = device.hardwareInfo.boardType;
                        Integer i = data.getOrDefault(key, 0);
                        data.put(key, ++i);
                    }
                }
            }
        }
        return data;
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

                String key = String.valueOf(sum);
                Integer i = filledSpace.getOrDefault(key, 0);
                filledSpace.put(key, ++i);
            }
        }
        return filledSpace;
    }

    public Map<String, Integer> getWebHookHosts() {
        Map<String, Integer> data = new HashMap<>();
        for (User user : users.values()) {
            for (DashBoard dashBoard : user.profile.dashBoards) {
                for (Widget widget : dashBoard.widgets) {
                    if (widget instanceof WebHook) {
                        WebHook webHook = (WebHook) widget;
                        if (webHook.url != null) {
                            try {
                                String key = getHost(webHook.url);
                                Integer i = data.getOrDefault(key, 0);
                                data.put(key, ++i);
                            } catch (Exception e) {
                                //don't care if we couldn't parse.
                            }
                        }
                    }
                }
            }
        }
        return data;
    }

    public void createProjectForExportedApp(TimerWorker timerWorker,
                                            TokenManager tokenManager,
                                            User newUser, String appName, int msgId) {
        if (appName.equals(AppNameUtil.BLYNK)) {
            return;
        }

        User parentUser = null;
        App app = null;

        for (User user : users.values()) {
            app = user.profile.getAppById(appName);
            if (app != null) {
                parentUser = user;
                break;
            }
        }

        if (app == null) {
            log.error("Unable to find app with id {}", appName);
            return;
        }

        if (app.isMultiFace) {
            log.info("App supports multi faces. Skipping profile creation.");
            return;
        }

        int dashId = app.projectIds[0];
        DashBoard dash = parentUser.profile.getDashByIdOrThrow(dashId);

        //todo ugly, but quick. refactor
        DashBoard clonedDash = JsonParser.parseDashboard(JsonParser.toJsonRestrictiveDashboard(dash), msgId);

        clonedDash.id = 1;
        clonedDash.parentId = dash.parentId;
        clonedDash.createdAt = System.currentTimeMillis();
        clonedDash.updatedAt = clonedDash.createdAt;
        clonedDash.isActive = true;
        clonedDash.eraseWidgetValues();
        removeDevicesProvisionedFromDeviceTiles(clonedDash);

        clonedDash.addTimers(timerWorker, new UserKey(newUser));

        newUser.profile.dashBoards = new DashBoard[] {clonedDash};

        if (app.provisionType == ProvisionType.STATIC) {
            for (Device device : clonedDash.devices) {
                device.erase();
            }
        } else {
            for (Device device : clonedDash.devices) {
                device.erase();
                String token = TokenGeneratorUtil.generateNewToken();
                tokenManager.assignToken(newUser, clonedDash, device, token);
            }
        }
    }

    //removes devices that has no widgets assigned to
    //probably those devices were added via device tiles widget
    private static void removeDevicesProvisionedFromDeviceTiles(DashBoard dash) {
        List<Device> list = new ArrayList<>(Arrays.asList(dash.devices));
        list.removeIf(device -> !dash.hasWidgetsByDeviceId(device.id));
        dash.devices = list.toArray(new Device[0]);
    }


    /**
     * Will take a url such as http://www.stackoverflow.com and return www.stackoverflow.com
     */
    private static String getHost(String url) {
        if (url == null || url.length() == 0) {
            return "";
        }

        int doubleslash = url.indexOf("//");
        if (doubleslash == -1) {
            doubleslash = 0;
        } else {
            doubleslash += 2;
        }

        int end = url.indexOf('/', doubleslash);
        end = end >= 0 ? end : url.length();

        int port = url.indexOf(':', doubleslash);
        end = (port > 0 && port < end) ? port : end;

        return url.substring(doubleslash, end);
    }

    public User addFacebookUser(String email, String appName) {
        log.debug("Adding new facebook user {}. App : {}", email, appName);
        User newUser = new User(email, null, appName, region, host, true, false);
        users.put(new UserKey(email, appName), newUser);
        return newUser;
    }

    public User add(String email, String passHash, String appName) {
        log.debug("Adding new user {}. App : {}", email, appName);
        User newUser = new User(email, passHash, appName, region, host, false, false);
        users.put(new UserKey(email, appName), newUser);
        return newUser;
    }

    public void add(String email, String passHash, String appName, boolean isSuperAdmin) {
        log.debug("Adding new user {}. App : {}", email, appName);
        User newUser = new User(email, passHash, appName, region, host, false, isSuperAdmin);
        users.put(new UserKey(email, appName), newUser);
    }

}
