package cc.blynk.server.core.model.widgets.ui;

import cc.blynk.server.core.model.widgets.NoPinWidget;
import cc.blynk.server.core.model.widgets.Target;
import cc.blynk.server.core.model.widgets.outputs.graph.FontSize;

import static cc.blynk.server.internal.EmptyArraysUtil.EMPTY_INTS;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 02.02.17.
 */
public class DeviceSelector extends NoPinWidget implements Target {

    public static final int DEVICE_SELECTOR_STARTING_ID = 200_000;

    //this is selected deviceId in widget
    public volatile int value = 0;

    public int[] deviceIds = EMPTY_INTS;

    public FontSize fontSize;

    public int iconColor;

    public String hint;

    @Override
    public int[] getDeviceIds() {
        return new int[] {value};
    }

    @Override
    public boolean isSelected(int deviceId) {
        return value == deviceId;
    }

    @Override
    public int[] getAssignedDeviceIds() {
        return deviceIds;
    }

    @Override
    public int getDeviceId() {
        return value;
    }

    @Override
    public int getPrice() {
        return 1900;
    }

}
