package cc.blynk.utils;

import cc.blynk.server.model.DashBoard;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 04.01.16.
 */
public class ArrayUtil {

    public static DashBoard[] add(final DashBoard[] array, final DashBoard element) {
        DashBoard[] newArray = copyArrayGrow1(array);
        newArray[newArray.length - 1] = element;
        return newArray;
    }

    private static DashBoard[] copyArrayGrow1(final DashBoard[] array) {
        final DashBoard[] newArray = new DashBoard[array.length + 1];
        System.arraycopy(array, 0, newArray, 0, array.length);
        return newArray;
    }

    public static DashBoard[] remove(final DashBoard[] array, final int index) {
        if (index < 0 || index >= array.length) {
            throw new IndexOutOfBoundsException("Index: " + index + ", Length: " + array.length);
        }

        final DashBoard[] result = new DashBoard[array.length - 1];
        System.arraycopy(array, 0, result, 0, index);
        if (index < array.length - 1) {
            System.arraycopy(array, index + 1, result, index, array.length - index - 1);
        }

        return result;
    }

}
