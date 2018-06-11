package cc.blynk.server.core.model.widgets;

import cc.blynk.utils.ArrayUtil;

import static cc.blynk.server.internal.EmptyArraysUtil.EMPTY_INTS;

public interface DeviceCleaner {

    void deleteDevice(int deviceId);

    default int[] deleteDeviceFromArray(int[] localDeviceIds, int deviceId) {
        int index = ArrayUtil.getIndexByVal(localDeviceIds, deviceId);
        if (index != -1) {
            return localDeviceIds.length == 1 ? EMPTY_INTS : ArrayUtil.remove(localDeviceIds, index);
        }
        return localDeviceIds;
    }

}
