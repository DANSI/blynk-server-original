package cc.blynk.common.model.messages.protocol.hardware;

import cc.blynk.common.model.messages.StringMessage;

import static cc.blynk.common.enums.Command.*;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 2/1/2015.
 */
public class HardwareSyncMessage extends StringMessage {

    public HardwareSyncMessage(int messageId) {
        super(messageId, HARDWARE_SYNC, 0, "");
    }

    @Override
    public String toString() {
        return "HardwareSyncMessage{" + super.toString() + "}";
    }
}
