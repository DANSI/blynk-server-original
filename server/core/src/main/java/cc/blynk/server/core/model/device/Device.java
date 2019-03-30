package cc.blynk.server.core.model.device;

import cc.blynk.server.core.model.serialization.JsonParser;
import cc.blynk.server.core.model.serialization.View;
import cc.blynk.server.core.model.widgets.Target;
import com.fasterxml.jackson.annotation.JsonView;

import static cc.blynk.server.core.model.device.HardwareInfo.DEFAULT_HARDWARE_BUFFER_SIZE;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 16.11.16.
 */
public class Device implements Target {

    public int id;

    public volatile String name;

    public volatile BoardType boardType;

    //used for bluetooth
    public volatile String address;

    @JsonView(View.Private.class)
    public volatile String token;

    public volatile String vendor;

    public volatile ConnectionType connectionType;

    @JsonView(View.Private.class)
    public volatile Status status = Status.OFFLINE;

    @JsonView(View.Private.class)
    public volatile long disconnectTime;

    @JsonView(View.Private.class)
    public volatile long connectTime;

    @JsonView(View.Private.class)
    public volatile long firstConnectTime;

    @JsonView(View.Private.class)
    public volatile long dataReceivedAt;

    @JsonView(View.Private.class)
    public volatile String lastLoggedIP;

    @JsonView(View.Private.class)
    public volatile HardwareInfo hardwareInfo;

    @JsonView(View.Private.class)
    public volatile DeviceOtaInfo deviceOtaInfo;

    public volatile String iconName;

    public volatile boolean isUserIcon;

    public Device(int id, String name, BoardType boardType) {
        this.id = id;
        this.name = name;
        this.boardType = boardType;
    }

    public Device() {
    }

    public boolean isNotValid() {
        return boardType == null || (name != null && name.length() > 50);
    }

    @Override
    public int[] getDeviceIds() {
        return new int[] {id};
    }

    @Override
    public boolean isSelected(int deviceId) {
        return id == deviceId;
    }

    @Override
    public int[] getAssignedDeviceIds() {
        return new int[] {id};
    }

    @Override
    public boolean contains(int deviceId) {
        return this.id == deviceId;
    }

    @Override
    public int getDeviceId() {
        return id;
    }

    public void update(Device newDevice) {
        this.name = newDevice.name;
        this.vendor = newDevice.vendor;
        this.boardType = newDevice.boardType;
        this.address = newDevice.address;
        this.connectionType = newDevice.connectionType;
        this.iconName = newDevice.iconName;
        this.isUserIcon = newDevice.isUserIcon;
        //that's fine. leave this fields as it is. It cannot be update from app client.
        //this.hardwareInfo = newDevice.hardwareInfo;
        //this.deviceOtaInfo = newDevice.deviceOtaInfo;
    }

    public void disconnected() {
        this.status = Status.OFFLINE;
        this.disconnectTime = System.currentTimeMillis();
    }

    public void connected() {
        this.status = Status.ONLINE;
        this.connectTime = System.currentTimeMillis();
    }

    public void erase() {
        this.token = null;
        this.disconnectTime = 0;
        this.connectTime = 0;
        this.firstConnectTime = 0;
        this.dataReceivedAt = 0;
        this.lastLoggedIP = null;
        this.status = Status.OFFLINE;
        this.hardwareInfo = null;
        this.deviceOtaInfo = null;
    }

    public String getNameOrDefault() {
        return name == null ? "New Device" : name;
    }

    //for single device update device always updated when ota is initiated.
    public void updateOTAInfo(String initiatedBy) {
        long now = System.currentTimeMillis();
        this.deviceOtaInfo = new DeviceOtaInfo(initiatedBy, now, now);
    }

    public boolean fitsBufferSize(int bodySize) {
        if (hardwareInfo == null) {
            return bodySize <= DEFAULT_HARDWARE_BUFFER_SIZE;
        }
        return bodySize + 5 <= hardwareInfo.buffIn;
    }

    @Override
    public String toString() {
        return JsonParser.toJson(this);
    }
}
