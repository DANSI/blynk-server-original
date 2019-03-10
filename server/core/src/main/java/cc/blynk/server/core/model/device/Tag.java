package cc.blynk.server.core.model.device;

import cc.blynk.server.core.model.serialization.JsonParser;
import cc.blynk.server.core.model.widgets.DeviceCleaner;
import cc.blynk.server.core.model.widgets.Target;
import cc.blynk.utils.ArrayUtil;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import static cc.blynk.server.internal.EmptyArraysUtil.EMPTY_INTS;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 16.11.16.
 */
public class Tag implements Target, DeviceCleaner {

    public static final int START_TAG_ID = 100_000;
    private static final int MAX_NUMBER_OF_DEVICE_PER_TAG = 25;

    public final int id;

    public volatile String name;

    public volatile int[] deviceIds;

    public boolean isNotValid() {
        return name == null || name.isEmpty() || name.length() > 40
                || id < START_TAG_ID || deviceIds.length > MAX_NUMBER_OF_DEVICE_PER_TAG;
    }

    public Tag(int id, String name) {
        this.id = id;
        this.name = name;
        this.deviceIds = EMPTY_INTS;
    }

    @JsonCreator
    public Tag(@JsonProperty("id") int id,
               @JsonProperty("name") String name,
               @JsonProperty("deviceIds") int[] deviceIds) {
        this.id = id;
        this.name = name;
        this.deviceIds = deviceIds == null ? EMPTY_INTS : deviceIds;
    }

    @Override
    public int[] getDeviceIds() {
        return deviceIds;
    }

    @Override
    public boolean isSelected(int deviceId) {
        return ArrayUtil.contains(deviceIds, deviceId);
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
        return deviceIds[0];
    }

    @Override
    public boolean isTag() {
        return true;
    }

    public void update(Tag tag) {
        this.name = tag.name;
        this.deviceIds = tag.deviceIds;
    }

    public Tag copy() {
        return new Tag(id, name, deviceIds);
    }

    @Override
    public void deleteDevice(int deviceId) {
        this.deviceIds = ArrayUtil.deleteFromArray(this.deviceIds, deviceId);
    }

    @Override
    public String toString() {
        return JsonParser.toJson(this);
    }
}
