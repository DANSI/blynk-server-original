package cc.blynk.server.core.protocol.model.messages.appllication;

import cc.blynk.server.core.protocol.model.messages.StringMessage;

import static cc.blynk.server.core.protocol.enums.Command.HARDWARE_RESEND_FROM_BLUETOOTH;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 2/1/2015.
 */
public class HardwareResendFromBluetoothMessage extends StringMessage {

    public HardwareResendFromBluetoothMessage(int messageId, String body) {
        super(messageId, HARDWARE_RESEND_FROM_BLUETOOTH, body.length(), body);
    }

    @Override
    public String toString() {
        return "HardwareResendFromBluetoothMessage{" + super.toString() + "}";
    }
}
