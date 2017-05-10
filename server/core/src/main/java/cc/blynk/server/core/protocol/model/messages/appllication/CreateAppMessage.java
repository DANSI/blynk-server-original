package cc.blynk.server.core.protocol.model.messages.appllication;

import cc.blynk.server.core.protocol.model.messages.StringMessage;

import static cc.blynk.server.core.protocol.enums.Command.CREATE_APP;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 2/1/2015.
 */
public class CreateAppMessage extends StringMessage {

    public CreateAppMessage(int messageId, String body) {
        super(messageId, CREATE_APP, body.length(), body);
    }

    @Override
    public String toString() {
        return "CreateAppMessage{" + super.toString() + "}";
    }
}
