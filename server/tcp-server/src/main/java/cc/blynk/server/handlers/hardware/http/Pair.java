package cc.blynk.server.handlers.hardware.http;

import java.util.Comparator;
import java.util.Map;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 07.12.15.
 */
public class Pair {

    public String name;
    public static Comparator<Pair> byName = (e1, e2) -> e1.name.compareTo(e2.name);
    public static Comparator<Pair> byNameAsInt = (e1, e2) -> Integer.valueOf(e1.name).compareTo(Integer.valueOf(e2.name));
    public Long count;
    public static Comparator<Pair> byCount = (e1, e2) -> e1.count.compareTo(e2.count);

    public Pair(Map.Entry<String, ?> entry) {
        this.name = entry.getKey();
        this.count = ((Number) entry.getValue()).longValue();
    }

}
