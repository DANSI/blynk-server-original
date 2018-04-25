package cc.blynk.utils;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.List;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 04.01.16.
 */
public final class ArrayUtil {

    private ArrayUtil() {
    }

    public static <T> T[] add(T[] array, T element, Class<T> type) {
        var newArray = copyArrayGrow1(array, type);
        newArray[newArray.length - 1] = element;
        return newArray;
    }

    @SuppressWarnings("unchecked")
    private static <T> T[] copyArrayGrow1(final T[] array, Class<T> type) {
        var newArray = (T[]) Array.newInstance(type, array.length + 1);
        System.arraycopy(array, 0, newArray, 0, array.length);
        return newArray;
    }

    @SuppressWarnings("unchecked")
    public static <T> T[] remove(final T[] array, final int index, Class<T> type) {
        var result = (T[]) Array.newInstance(type, array.length - 1);
        System.arraycopy(array, 0, result, 0, index);
        if (index < array.length - 1) {
            System.arraycopy(array, index + 1, result, index, array.length - index - 1);
        }

        return result;
    }

    public static <T> T[] copyAndReplace(T[] array, T element, int index) {
        var newArray = Arrays.copyOf(array, array.length);
        newArray[index] = element;
        return newArray;
    }

    public static boolean contains(final int[] ar, final int val) {
        for (var arVal : ar) {
            if (arVal == val) {
                return true;
            }
        }
        return false;
    }

    public static int[] convertIntegersToInt(List<Integer> integers) {
        var result = new int[integers.size()];
        for (int i = 0; i < result.length; i++) {
            result[i] = integers.get(i);
        }
        return result;
    }

}
