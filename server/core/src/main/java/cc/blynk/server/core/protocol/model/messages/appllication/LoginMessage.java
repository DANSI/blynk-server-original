package cc.blynk.server.core.protocol.model.messages.appllication;

import cc.blynk.server.core.protocol.model.messages.StringMessage;

import static cc.blynk.server.core.protocol.enums.Command.LOGIN;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 2/1/2015.
 */
public class LoginMessage extends StringMessage {

    public LoginMessage(int messageId, String body) {
        super(messageId, LOGIN, body);
    }

    public LoginMessage(int messageId, short command, String body) {
        super(messageId, command, body);
    }

    @Override
    public String toString() {
        return "LoginMessage{" + super.toString() + "}";
    }
}
