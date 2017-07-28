package cc.blynk.server.core.protocol.model.messages.appllication;

import cc.blynk.server.core.protocol.model.messages.StringMessage;

import static cc.blynk.server.core.protocol.enums.Command.GET_PROJECT_BY_CLONE_CODE;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 2/1/2015.
 */
public class GetProjectByCloneCodeStringMessage extends StringMessage {

    public GetProjectByCloneCodeStringMessage(int messageId, String body) {
        super(messageId, GET_PROJECT_BY_CLONE_CODE, body.length(), body);
    }

    @Override
    public String toString() {
        return "GetProjectByCloneCodeStringMessage{" + super.toString() + "}";
    }
}
