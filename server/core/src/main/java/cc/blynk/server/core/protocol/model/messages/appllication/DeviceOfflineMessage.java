package cc.blynk.server.core.protocol.model.messages.appllication;

import cc.blynk.server.core.protocol.model.messages.StringMessage;

import static cc.blynk.server.core.protocol.enums.Command.DEVICE_OFFLINE;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 2/1/2015.
 */
public class DeviceOfflineMessage extends StringMessage {

    public DeviceOfflineMessage(int messageId, String body) {
        super(messageId, DEVICE_OFFLINE, body.length(), body);
    }

    @Override
    public String toString() {
        return "DeviceOfflineMessage{" + super.toString() + "}";
    }
}
