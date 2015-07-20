package cc.blynk.server.workers;

import cc.blynk.server.utils.JsonParser;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 19.07.15.
 */
public class Stat {

    //2015-07-20T20:15:03.954+03:00
    String ts = OffsetDateTime.now().toString();
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
