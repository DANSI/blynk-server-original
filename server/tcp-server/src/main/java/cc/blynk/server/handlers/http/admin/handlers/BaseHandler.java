package cc.blynk.server.handlers.http.admin.handlers;

import cc.blynk.server.handlers.http.admin.response.NameCountResponse;
import cc.blynk.server.utils.GenericComparator;
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
public abstract class BaseHandler {

    static final Logger log = LogManager.getLogger(BaseHandler.class);

    static List<?> sort(List<?> list, String field, String order, boolean nameAsInt) {
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

    static List<?> sort(List<?> list, String field, String order) {
        return sort(list, field, order, false);
    }

    static List<NameCountResponse> convertMapToPair(Map<String, ?> map) {
        return map.entrySet().stream().map(NameCountResponse::new).collect(Collectors.toList());
    }


}
