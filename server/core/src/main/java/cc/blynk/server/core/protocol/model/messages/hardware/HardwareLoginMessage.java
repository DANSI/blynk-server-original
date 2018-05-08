package cc.blynk.server.core.protocol.model.messages.hardware;

import cc.blynk.server.core.protocol.model.messages.appllication.LoginMessage;

import static cc.blynk.server.core.protocol.enums.Command.HARDWARE_LOGIN;

public class HardwareLoginMessage extends LoginMessage {

    public HardwareLoginMessage(int messageId, String body) {
        super(messageId, HARDWARE_LOGIN, body);
    }

    @Override
    public String toString() {
        return "HardwareLoginMessage{" + super.toString() + "}";
    }

}
