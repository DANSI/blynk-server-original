package cc.blynk.server.core.model.device;

import cc.blynk.server.core.model.widgets.Target;
import cc.blynk.utils.JsonParser;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 16.11.16.
 */
public class Device implements Target {

    public int id;

    public volatile String name;

    public volatile String boardType;

    public volatile String token;

    public volatile ConnectionType connectionType;

    public volatile Status status = Status.OFFLINE;

    public volatile long disconnectTime;

    public volatile String lastLoggedIP;

    public volatile HardwareInfo hardwareInfo;

    public volatile DeviceOtaInfo deviceOtaInfo;

    public boolean isNotValid() {
        return boardType == null || boardType.isEmpty() || boardType.length() > 50 || (name != null && name.length() > 50);
    }

    public Device() {
    }

    public Device(int id, String name, String boardType) {
        this.id = id;
        this.name = name;
        this.boardType = boardType;
    }

    @Override
    public int[] getDeviceIds() {
        return new int[] {id};
    }

    @Override
    public int getDeviceId() {
        return id;
    }

    public void update(Device newDevice) {
        this.name = newDevice.name;
        this.boardType = newDevice.boardType;
        this.connectionType = newDevice.connectionType;
        //that's fine. leave this fields as it is. It cannot be update from app client.
        //this.hardwareInfo = newDevice.hardwareInfo;
        //this.deviceOtaInfo = newDevice.deviceOtaInfo;
    }

    public void disconnected() {
        this.status = Status.OFFLINE;
        this.disconnectTime = System.currentTimeMillis();
    }

    public void erase() {
        this.token = null;
        this.disconnectTime = 0;
        this.lastLoggedIP = null;
        this.status = Status.OFFLINE;
        this.hardwareInfo = null;
        this.deviceOtaInfo = null;
    }

    public void updateOTAInfo(String initiatedBy) {
        long OTAUpdateAt = deviceOtaInfo == null ? 0 : deviceOtaInfo.OTAUpdateAt;
        this.deviceOtaInfo = new DeviceOtaInfo(initiatedBy, System.currentTimeMillis(), OTAUpdateAt);
    }

    public void connected() {
        this.status = Status.ONLINE;
    }

    @Override
    public String toString() {
        return JsonParser.toJson(this);
    }
}
