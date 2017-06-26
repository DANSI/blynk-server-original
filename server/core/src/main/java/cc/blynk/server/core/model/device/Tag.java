package cc.blynk.server.core.model.device;

import cc.blynk.server.core.model.widgets.Target;
import cc.blynk.utils.ArrayUtil;
import cc.blynk.utils.JsonParser;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 16.11.16.
 */
public class Tag implements Target {

    public static final int START_TAG_ID = 100_000;

    public final int id;

    public volatile String name;

    public volatile int[] deviceIds;

    public boolean isNotValid() {
        return name == null || name.isEmpty() || name.length() > 40 || id < START_TAG_ID || deviceIds.length > 100;
    }

    public Tag(int id, String name) {
        this.id = id;
        this.name = name;
        this.deviceIds = ArrayUtil.EMPTY_INTS;
    }

    @JsonCreator
    public Tag(@JsonProperty("id") int id,
               @JsonProperty("name") String name,
               @JsonProperty("deviceIds") int[] deviceIds) {
        this.id = id;
        this.name = name;
        this.deviceIds = deviceIds == null ? ArrayUtil.EMPTY_INTS : deviceIds;
    }

    @Override
    public int[] getDeviceIds() {
        return deviceIds;
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
    public String toString() {
        return JsonParser.toJson(this);
    }
}
