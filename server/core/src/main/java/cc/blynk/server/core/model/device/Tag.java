package cc.blynk.server.core.model.device;

import cc.blynk.utils.JsonParser;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 16.11.16.
 */
public class Tag {

    public int id;

    public volatile String name;

    public volatile int[] deviceIds;

    public Tag() {
    }

    public Tag(int id, String name) {
        this.id = id;
        this.name = name;
    }

    public void update(Tag tag) {
        this.name = tag.name;
        this.deviceIds = tag.deviceIds;
    }

    @Override
    public String toString() {
        return JsonParser.toJson(this);
    }
}
