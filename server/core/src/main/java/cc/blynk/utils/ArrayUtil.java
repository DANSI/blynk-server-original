package cc.blynk.utils;

import cc.blynk.server.core.model.DashBoard;
import cc.blynk.server.core.model.Device;
import cc.blynk.server.core.model.widgets.Widget;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 04.01.16.
 */
public class ArrayUtil {

    public static Device[] add(final Device[] array, final Device element) {
        Device[] newArray = copyArrayGrow1(array);
        newArray[newArray.length - 1] = element;
        return newArray;
    }

    public static DashBoard[] add(final DashBoard[] array, final DashBoard element) {
        DashBoard[] newArray = copyArrayGrow1(array);
        newArray[newArray.length - 1] = element;
        return newArray;
    }

    public static Widget[] add(final Widget[] array, final Widget element) {
        Widget[] newArray = copyArrayGrow1(array);
        newArray[newArray.length - 1] = element;
        return newArray;
    }

    private static DashBoard[] copyArrayGrow1(final DashBoard[] array) {
        final DashBoard[] newArray = new DashBoard[array.length + 1];
        System.arraycopy(array, 0, newArray, 0, array.length);
        return newArray;
    }

    private static Widget[] copyArrayGrow1(final Widget[] array) {
        final Widget[] newArray = new Widget[array.length + 1];
        System.arraycopy(array, 0, newArray, 0, array.length);
        return newArray;
    }

    private static Device[] copyArrayGrow1(final Device[] array) {
        final Device[] newArray = new Device[array.length + 1];
        System.arraycopy(array, 0, newArray, 0, array.length);
        return newArray;
    }

    public static DashBoard[] remove(final DashBoard[] array, final int index) {
        final DashBoard[] result = new DashBoard[array.length - 1];
        System.arraycopy(array, 0, result, 0, index);
        if (index < array.length - 1) {
            System.arraycopy(array, index + 1, result, index, array.length - index - 1);
        }

        return result;
    }

    public static Widget[] remove(final Widget[] array, final int index) {
        final Widget[] result = new Widget[array.length - 1];
        System.arraycopy(array, 0, result, 0, index);
        if (index < array.length - 1) {
            System.arraycopy(array, index + 1, result, index, array.length - index - 1);
        }

        return result;
    }

    public static Device[] remove(final Device[] array, final int index) {
        final Device[] result = new Device[array.length - 1];
        System.arraycopy(array, 0, result, 0, index);
        if (index < array.length - 1) {
            System.arraycopy(array, index + 1, result, index, array.length - index - 1);
        }

        return result;
    }

}
