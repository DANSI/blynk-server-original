package cc.blynk.common.model.messages.protocol.appllication.sharing;

import cc.blynk.common.enums.Command;
import cc.blynk.common.model.messages.StringMessage;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 05.11.15.
 */
public class SharingMessage extends StringMessage {

    public SharingMessage(int messageId, String body) {
        super(messageId, Command.SHARING, body.length(), body);
    }

    @Override
    public String toString() {
        return "SharingMessage{" + super.toString() + "}";
    }
}
