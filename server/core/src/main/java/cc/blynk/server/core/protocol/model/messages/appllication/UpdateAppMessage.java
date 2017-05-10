package cc.blynk.server.core.protocol.model.messages.appllication;

import cc.blynk.server.core.protocol.model.messages.StringMessage;

import static cc.blynk.server.core.protocol.enums.Command.UPDATE_APP;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 2/1/2015.
 */
public class UpdateAppMessage extends StringMessage {

    public UpdateAppMessage(int messageId, String body) {
        super(messageId, UPDATE_APP, body.length(), body);
    }

    @Override
    public String toString() {
        return "UpdateAppMessage{" + super.toString() + "}";
    }
}
