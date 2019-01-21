package cc.blynk.server.core.model.widgets.ui;

import cc.blynk.server.core.model.widgets.DeviceCleaner;
import cc.blynk.server.core.model.widgets.NoPinWidget;
import cc.blynk.server.core.model.widgets.Target;
import cc.blynk.server.core.model.widgets.outputs.graph.FontSize;
import cc.blynk.utils.ArrayUtil;

import static cc.blynk.server.internal.EmptyArraysUtil.EMPTY_INTS;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 02.02.17.
 */
public class DeviceSelector extends NoPinWidget implements Target, DeviceCleaner {

    public static final int DEVICE_SELECTOR_STARTING_ID = 200_000;

    //this is selected deviceId in widget
    public volatile int value = 0;

    public volatile int[] deviceIds = EMPTY_INTS;

    public FontSize fontSize;

    public int iconColor;

    public boolean showIcon;

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
    public boolean contains(int deviceId) {
        return ArrayUtil.contains(this.deviceIds, deviceId);
    }

    @Override
    public int getDeviceId() {
        return value;
    }

    @Override
    public int getPrice() {
        return 1900;
    }

    @Override
    public boolean isAssignedToDevice(int deviceId) {
        return ArrayUtil.contains(this.deviceIds, deviceId);
    }

    @Override
    public void deleteDevice(int deviceId) {
        this.deviceIds = ArrayUtil.deleteFromArray(this.deviceIds, deviceId);
    }
}
