package cc.blynk.server.core.model;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 16.11.16.
 */
public class Device {

    public int id;

    public String name;

    public String boardType;

    public String token;

    public ConnectionType connectionType;

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

}
