package cc.blynk.server.handlers.http.utils;

import cc.blynk.server.handlers.http.NameCountResponse;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

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
public class LogicHelper {

    public static final Logger log = LogManager.getLogger(LogicHelper.class);

    public static List<?> sort(List<?> list, String field, String order, boolean nameAsInt) {
        if (list.size() == 0) {
            return list;
        }

        try {
            Comparator c = new GenericComparator(list.get(0).getClass(), field, nameAsInt);
            Collections.sort(list, "ASC".equals(order) ? c : Collections.reverseOrder(c));
        } catch (NoSuchFieldException e) {
            log.warn("No order field '{}'", field);
        } catch (Exception e) {
            log.error("Problem sorting.", e);
        }

        return list;
    }

    public static List<?> sort(List<?> list, String field, String order) {
        return sort(list, field, order, false);
    }

    public static List<NameCountResponse> convertMapToPair(Map<String, ?> map) {
        return map.entrySet().stream().map(NameCountResponse::new).collect(Collectors.toList());
    }


}
