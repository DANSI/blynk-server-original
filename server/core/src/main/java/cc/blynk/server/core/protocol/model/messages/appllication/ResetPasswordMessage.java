package cc.blynk.server.core.protocol.model.messages.appllication;

import cc.blynk.server.core.protocol.model.messages.StringMessage;

import static cc.blynk.server.core.protocol.enums.Command.RESET_PASSWORD;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 2/1/2015.
 */
public class ResetPasswordMessage extends StringMessage {

    public ResetPasswordMessage(int messageId, String body) {
        super(messageId, RESET_PASSWORD, body);
    }

    @Override
    public String toString() {
        return "ResetPasswordMessage{" + super.toString() + "}";
    }
}
