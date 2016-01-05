package cc.blynk.utils;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 06.12.15.
 */
public class ListUtils {

    public static List<?> subList(Collection<?> list, int page, int size) {
        return list.stream()
                .skip((page - 1)  * size)
                .limit(size)
                .collect(Collectors.toList());
    }

}
