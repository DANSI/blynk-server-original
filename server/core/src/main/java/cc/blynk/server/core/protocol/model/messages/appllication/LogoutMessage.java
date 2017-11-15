package cc.blynk.server.core.protocol.model.messages.appllication;

import cc.blynk.server.core.protocol.model.messages.StringMessage;

import static cc.blynk.server.core.protocol.enums.Command.LOGOUT;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 2/1/2015.
 */
public class LogoutMessage extends StringMessage {

    public LogoutMessage(int messageId, String body) {
        super(messageId, LOGOUT, body.length(), body);
    }

    @Override
    public String toString() {
        return "LogoutMessage{" + super.toString() + "}";
    }
}
