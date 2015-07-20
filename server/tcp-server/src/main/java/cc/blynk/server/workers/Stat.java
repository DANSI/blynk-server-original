package cc.blynk.server.workers;

import cc.blynk.server.utils.JsonParser;

import java.util.HashMap;
import java.util.Map;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 19.07.15.
 */
public class Stat {

    long ts = System.currentTimeMillis();
    long oneMinRate;
    long total;
    long active;
    long connected;
    long onlineApps;
    long onlineHards;

    Map<String, Long> messages = new HashMap<>();

    @Override
    public String toString() {
        return JsonParser.toJson(this);
    }
}
