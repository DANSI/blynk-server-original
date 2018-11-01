package cc.blynk.utils;

import java.lang.reflect.Array;
import java.util.Arrays;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 04.01.16.
 */
public final class ArrayUtil {

    private static final int[] EMPTY_INTS = {};

    private ArrayUtil() {
    }

    public static <T> T[] add(T[] array, T element, Class<T> type) {
        T[] newArray = copyArrayGrow1(array, type);
        newArray[newArray.length - 1] = element;
        return newArray;
    }

    @SuppressWarnings("unchecked")
    private static <T> T[] copyArrayGrow1(final T[] array, Class<T> type) {
        T[] newArray = (T[]) Array.newInstance(type, array.length + 1);
        System.arraycopy(array, 0, newArray, 0, array.length);
        return newArray;
    }

    @SuppressWarnings("unchecked")
    public static <T> T[] remove(final T[] array, final int index, Class<T> type) {
        T[] result = (T[]) Array.newInstance(type, array.length - 1);
        System.arraycopy(array, 0, result, 0, index);
        if (index < array.length - 1) {
            System.arraycopy(array, index + 1, result, index, array.length - index - 1);
        }

        return result;
    }

    public static <T> T[] copyAndReplace(T[] array, T element, int index) {
        T[] newArray = Arrays.copyOf(array, array.length);
        newArray[index] = element;
        return newArray;
    }

    public static int getIndexByVal(int[] array, int val) {
        for (int i = 0; i < array.length; i++) {
            if (array[i] == val) {
                return i;
            }
        }
        return -1;
    }

    public static int[] deleteFromArray(int[] ids, int id) {
        int index = getIndexByVal(ids, id);
        if (index == -1) {
            return ids;
        }
        return ids.length == 1 ? EMPTY_INTS : remove(ids, index);
    }

    public static int[] remove(int[] array, int index) {
        int[] result = new int[array.length - 1];
        System.arraycopy(array, 0, result, 0, index);
        if (index < array.length - 1) {
            System.arraycopy(array, index + 1, result, index, array.length - index - 1);
        }

        return result;
    }

    public static boolean contains(final int[] ar, final int val) {
        for (int arVal : ar) {
            if (arVal == val) {
                return true;
            }
        }
        return false;
    }

}
