package cc.blynk.server.handlers.http.admin.handlers;

import cc.blynk.server.handlers.http.Pair;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 09.12.15.
 */
public abstract class BaseHandler {

    static List<Pair> sort(List<Pair> list, String field, String order, boolean nameAsInt) {
        Comparator<Pair> c = "name".equals(field) ? (nameAsInt ? Pair.byNameAsInt : Pair.byName) : Pair.byCount;
        Collections.sort(list, "ASC".equals(order) ? c : Collections.reverseOrder(c));
        return list;
    }

    static List<Pair> sort(List<Pair> list, String field, String order) {
        return sort(list, field, order, false);
    }

    static List<Pair> convertMapToPair(Map<String, ?> map) {
        return map.entrySet().stream().map(Pair::new).collect(Collectors.toList());
    }


}
