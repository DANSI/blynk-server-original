package cc.blynk.server.core.protocol.model.messages.appllication;

import cc.blynk.server.core.protocol.model.messages.StringMessage;

import static cc.blynk.server.core.protocol.enums.Command.UPDATE_FACE;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 2/1/2015.
 */
public class UpdateFaceMessage extends StringMessage {

    public UpdateFaceMessage(int messageId, String body) {
        super(messageId, UPDATE_FACE, body.length(), body);
    }

    @Override
    public String toString() {
        return "UpdateFaceMessage{" + super.toString() + "}";
    }
}
