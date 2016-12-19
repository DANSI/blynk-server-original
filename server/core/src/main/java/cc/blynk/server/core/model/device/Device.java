package cc.blynk.server.core.model.device;

import cc.blynk.utils.JsonParser;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 16.11.16.
 */
public class Device {

    public int id;

    public volatile String name;

    public volatile String boardType;

    public volatile String token;

    public volatile ConnectionType connectionType;

    public volatile transient Status status;

    public volatile long disconnectTime;

    public Device() {
    }

    public Device(int id, String name, String boardType, String token, ConnectionType connectionType) {
        this.id = id;
        this.name = name;
        this.boardType = boardType;
        this.token = token;
        this.connectionType = connectionType;
    }

    public Device(int id, String name, String boardType) {
        this.id = id;
        this.name = name;
        this.boardType = boardType;
    }

    public void update(Device newDevice) {
        this.name = newDevice.name;
        this.boardType = newDevice.boardType;
        this.connectionType = newDevice.connectionType;
    }

    public void disconnected() {
        this.status = Status.OFFLINE;
        this.disconnectTime = System.currentTimeMillis();
    }

    public void connected() {
        this.status = Status.ONLINE;
    }

    @Override
    public String toString() {
        return JsonParser.toJson(this);
    }
}
