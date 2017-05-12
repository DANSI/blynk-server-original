package cc.blynk.server.core.protocol.model.messages.appllication;

import cc.blynk.server.core.protocol.model.messages.StringMessage;

import static cc.blynk.server.core.protocol.enums.Command.GET_PROJECT_BY_TOKEN;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 2/1/2015.
 */
public class GetProjectByTokenStringMessage extends StringMessage {

    public GetProjectByTokenStringMessage(int messageId, String body) {
        super(messageId, GET_PROJECT_BY_TOKEN, body.length(), body);
    }

    @Override
    public String toString() {
        return "GetProjectByTokenStringMessage{" + super.toString() + "}";
    }
}
