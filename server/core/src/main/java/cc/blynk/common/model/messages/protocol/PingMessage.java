package cc.blynk.common.model.messages.protocol;

import cc.blynk.common.model.messages.StringMessage;

import static cc.blynk.common.enums.Command.*;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 2/1/2015.
 */
public class PingMessage extends StringMessage {

    public PingMessage(int messageId) {
        super(messageId, PING, 0, "");
    }

    @Override
    public String toString() {
        return "PingMessage{" + super.toString() + "}";
    }
}
