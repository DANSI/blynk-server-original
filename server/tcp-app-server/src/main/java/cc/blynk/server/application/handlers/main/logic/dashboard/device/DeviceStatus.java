package cc.blynk.server.application.handlers.main.logic.dashboard.device;

import cc.blynk.server.core.model.device.Device;
import cc.blynk.server.core.model.device.Status;

/**
 * This class is copy of device class but without transient field.
 * It is a bit ugly workaround, but quick.
 *
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 20.12.16.
 */
public class DeviceStatus extends Device {

    public Status status;

    public DeviceStatus() {
    }

    public DeviceStatus(Device device) {
        this.id = device.id;
        this.name = device.name;
        this.boardType = device.boardType;
        this.token = device.token;
        this.connectionType = device.connectionType;
        this.status = device.status;
        this.disconnectTime = device.disconnectTime;
    }
}
