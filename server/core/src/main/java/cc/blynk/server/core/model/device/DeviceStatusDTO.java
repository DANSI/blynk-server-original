package cc.blynk.server.core.model.device;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 18.09.18.
 */
public class DeviceStatusDTO {

    public final int id;

    public final String name;

    public final BoardType boardType;

    public final String token;

    public final String vendor;

    public final ConnectionType connectionType;

    public final Status status;

    public final long disconnectTime;

    public final long connectTime;

    public final long dataReceivedAt;

    public final HardwareInfo hardwareInfo;

    public final String iconName;

    public final boolean isUserIcon;

    public DeviceStatusDTO(Device device) {
        this.id = device.id;
        this.name = device.name;
        this.boardType = device.boardType;
        this.token = device.token;
        this.vendor = device.vendor;
        this.connectionType = device.connectionType;
        this.status = device.status;
        this.disconnectTime = device.disconnectTime;
        this.connectTime = device.connectTime;
        this.dataReceivedAt = device.dataReceivedAt;
        this.hardwareInfo = device.hardwareInfo;
        this.iconName = device.iconName;
        this.isUserIcon = device.isUserIcon;
    }

    public static DeviceStatusDTO[] transform(Device[] devices) {
        DeviceStatusDTO[] deviceStatusDTO = new DeviceStatusDTO[devices.length];
        for (int i = 0; i < devices.length; i++) {
            deviceStatusDTO[i] = new DeviceStatusDTO(devices[i]);
        }
        return deviceStatusDTO;
    }
}
