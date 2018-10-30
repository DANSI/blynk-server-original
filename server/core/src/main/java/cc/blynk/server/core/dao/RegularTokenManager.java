package cc.blynk.server.core.dao;

import cc.blynk.server.core.model.DashBoard;
import cc.blynk.server.core.model.auth.User;
import cc.blynk.server.core.model.device.Device;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 22.09.15.
 */
class RegularTokenManager {

    private static final Logger log = LogManager.getLogger(RegularTokenManager.class);

    final ConcurrentHashMap<String, TokenValue> cache;

    RegularTokenManager(Collection<User> users) {
        ///in average user has 2 devices
        this.cache = new ConcurrentHashMap<>(users.size() == 0 ? 16 : users.size() * 2);
        for (User user : users) {
            if (user.profile != null) {
                for (DashBoard dashBoard : user.profile.dashBoards) {
                    for (Device device : dashBoard.devices) {
                        if (device.token != null) {
                            cache.put(device.token, new TokenValue(user, dashBoard, device));
                        }
                    }
                }
            }
        }
    }

    String assignToken(User user, DashBoard dash, Device device, String newToken, boolean isTemporary) {
        // Clean old token from cache if exists.
        String oldToken = deleteDeviceToken(device.token);

        //assign new token
        device.token = newToken;
        TokenValue tokenValue = isTemporary
                ? new TemporaryTokenValue(user, dash, device)
                : new TokenValue(user, dash, device);
        cache.put(newToken, tokenValue);

        user.lastModifiedTs = System.currentTimeMillis();

        log.debug("Generated token for user {}, dashId {}, deviceId {} is {}.",
                user.email, dash.id, device.id, newToken);

        return oldToken;
    }

    String deleteDeviceToken(String deviceToken) {
        if (deviceToken != null) {
            cache.remove(deviceToken);
            return deviceToken;
        }
        return null;
    }

    TokenValue getUserByToken(String token) {
        return cache.get(token);
    }

    String[] deleteProject(DashBoard dash) {
        ArrayList<String> removedTokens = new ArrayList<>(dash.devices.length);
        for (Device device : dash.devices) {
            if (device != null && device.token != null) {
                cache.remove(device.token);
                removedTokens.add(device.token);
            }
        }
        return removedTokens.toArray(new String[0]);
    }

}
