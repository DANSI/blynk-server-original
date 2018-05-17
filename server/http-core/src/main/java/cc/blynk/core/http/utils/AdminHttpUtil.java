package cc.blynk.core.http.utils;

import cc.blynk.core.http.model.NameCountResponse;
import cc.blynk.server.core.model.serialization.JsonParser;
import cc.blynk.server.core.stats.model.CommandStat;

import java.lang.reflect.Field;
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
public final class AdminHttpUtil {

    private AdminHttpUtil() {
    }

    @SuppressWarnings("unchecked")
    public static List<?> sortStringAsInt(List<?> list, String field, String order) {
        if (list.size() == 0) {
            return list;
        }

        Comparator c = new GenericStringAsIntComparator(list.get(0).getClass(), field);
        list.sort("asc".equalsIgnoreCase(order) ? c : Collections.reverseOrder(c));

        return list;
    }

    @SuppressWarnings("unchecked")
    public static List<?> sort(List<?> list, String field, String order) {
        if (list.size() == 0) {
            return list;
        }

        Comparator c = new GenericComparator(list.get(0).getClass(), field);
        list.sort("asc".equalsIgnoreCase(order) ? c : Collections.reverseOrder(c));

        return list;
    }

    public static List<NameCountResponse> convertMapToPair(Map<String, ?> map) {
        return map.entrySet().stream().map(NameCountResponse::new).collect(Collectors.toList());
    }

    @SuppressWarnings("unchecked")
    public static List<NameCountResponse> convertObjectToMap(CommandStat commandStat) {
        return convertMapToPair(JsonParser.MAPPER.convertValue(commandStat, Map.class));
    }

    /**
     * The Blynk Project.
     * Created by Dmitriy Dumanskiy.
     * Created on 10.12.15.
     */
    public static class GenericComparator implements Comparator {

        private final Class<?> fieldType;
        private final Field field;

        GenericComparator(Class<?> type, String sortField) {
            try {
                this.field = type.getField(sortField);
            } catch (NoSuchFieldException nsfe) {
                throw new RuntimeException("Can't find field " + sortField + " for " + type.getName());
            }
            this.fieldType = field.getType();
        }

        @Override
        public int compare(Object o1, Object o2) {
            try {
                Object v1 = field.get(o1);
                Object v2 = field.get(o2);

                return compareActual(v1, v2, fieldType);
            } catch (Exception e) {
                throw new RuntimeException("Error on compare during sorting. Type : " + e.getMessage());
            }
        }

        public int compareActual(Object v1, Object v2, Class<?> returnType) {
            if (returnType == int.class || returnType == Integer.class) {
                return Integer.compare((int) v1, (int) v2);
            }
            if (returnType == long.class || returnType == Long.class) {
                return Long.compare((long) v1, (long) v2);
            }
            if (returnType == String.class) {
                return ((String) v1).compareTo((String) v2);
            }

            throw new RuntimeException("Unexpected field type. Type : " + returnType.getName());
        }

    }

    public static class GenericStringAsIntComparator extends GenericComparator {

        GenericStringAsIntComparator(Class<?> type, String sortField) {
            super(type, sortField);
        }

        @Override
        public int compareActual(Object v1, Object v2, Class<?> returnType) {
            if (returnType == int.class || returnType == Integer.class) {
                return Integer.compare((int) v1, (int) v2);
            }
            if (returnType == long.class || returnType == Long.class) {
                return Long.compare((long) v1, (long) v2);
            }
            if (returnType == String.class) {
                return Integer.valueOf((String) v1).compareTo(Integer.valueOf((String) v2));
            }

            throw new RuntimeException("Unexpected field type. Type : " + returnType.getName());
        }

    }
}
