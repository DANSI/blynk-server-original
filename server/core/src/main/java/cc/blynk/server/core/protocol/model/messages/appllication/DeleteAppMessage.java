package cc.blynk.server.core.protocol.model.messages.appllication;

import cc.blynk.server.core.protocol.model.messages.StringMessage;

import static cc.blynk.server.core.protocol.enums.Command.DELETE_APP;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 2/1/2015.
 */
public class DeleteAppMessage extends StringMessage {

    public DeleteAppMessage(int messageId, String body) {
        super(messageId, DELETE_APP, body.length(), body);
    }

    @Override
    public String toString() {
        return "DeleteAppMessage{" + super.toString() + "}";
    }
}
