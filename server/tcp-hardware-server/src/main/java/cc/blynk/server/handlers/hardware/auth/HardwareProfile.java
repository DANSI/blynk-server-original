package cc.blynk.server.handlers.hardware.auth;

import java.util.HashMap;
import java.util.Map;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 25.10.15.
 */
public class HardwareProfile {

    private static final String HEART_BEAT_INTERVAL = "h-beat";

    private final Map<String, String> infos;

    public HardwareProfile(String[] info) {
        this.infos = new HashMap<>();
        for (int i = 0; i < info.length; i++) {
            if (i < info.length - 1) {
                infos.put(info[i], info[++i]);
            }
        }
    }

    public int getHeartBeatInterval() {
        try {
            return Integer.parseInt(infos.get(HEART_BEAT_INTERVAL));
        } catch (NumberFormatException nfe) {
            return -1;
        }
    }

}
