package cc.blynk.server.core.protocol.model.messages.appllication;

import cc.blynk.server.core.protocol.model.messages.StringMessage;

import static cc.blynk.server.core.protocol.enums.Command.GET_CLONE_CODE;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 2/1/2015.
 */
public class GetCloneCodeMessage extends StringMessage {

    public GetCloneCodeMessage(int messageId, String body) {
        super(messageId, GET_CLONE_CODE, body.length(), body);
    }

    @Override
    public String toString() {
        return "GetCloneMessage{" + super.toString() + "}";
    }
}
