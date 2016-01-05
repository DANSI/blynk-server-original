package cc.blynk.common.model.messages.protocol.appllication.sharing;

import cc.blynk.common.enums.Command;
import cc.blynk.common.model.messages.StringMessage;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 27.10.15.
 */
public class SyncMessage extends StringMessage {

    public SyncMessage(int messageId, String body) {
        super(messageId, Command.SYNC, body.length(), body);
    }

    @Override
    public String toString() {
        return "SyncMessage{" + super.toString() + "}";
    }
}
