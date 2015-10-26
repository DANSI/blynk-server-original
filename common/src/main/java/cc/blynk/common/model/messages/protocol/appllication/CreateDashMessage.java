package cc.blynk.common.model.messages.protocol.appllication;

import cc.blynk.common.model.messages.StringMessage;

import static cc.blynk.common.enums.Command.CREATE_DASH;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 2/1/2015.
 */
public class CreateDashMessage extends StringMessage {

    public CreateDashMessage(int messageId, String body) {
        super(messageId, CREATE_DASH, body.length(), body);
    }

    @Override
    public String toString() {
        return "CreateDashMessage{" + super.toString() + "}";
    }
}
