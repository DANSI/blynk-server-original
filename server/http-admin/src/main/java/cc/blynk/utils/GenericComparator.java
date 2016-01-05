package cc.blynk.utils;

import java.lang.reflect.Field;
import java.util.Comparator;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 10.12.15.
 */
public class GenericComparator implements Comparator {

    private final Class<?> fieldType;
    private final Field field;
    private final boolean nameAsInt;

    public GenericComparator(Class<?> type, String sortField, boolean nameAsInt) throws NoSuchFieldException{
        this.field = type.getField(sortField);
        this.fieldType = field.getType();
        this.nameAsInt = nameAsInt;
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

    private int compareActual(Object v1, Object v2, Class<?> returnType) {
        if (returnType == int.class || returnType == Integer.class) {
            return Integer.compare((int) v1, (int) v2);
        }
        if (returnType == long.class || returnType == Long.class) {
            return Long.compare((long) v1, (long) v2);
        }
        if (returnType == String.class) {
            if (nameAsInt) {
                return Integer.valueOf((String) v1).compareTo(Integer.valueOf((String) v2));
            } else {
                return ((String) v1).compareTo((String) v2);
            }
        }

        throw new RuntimeException("Unexpected field type. Type : " + returnType.getName());
    }

}
