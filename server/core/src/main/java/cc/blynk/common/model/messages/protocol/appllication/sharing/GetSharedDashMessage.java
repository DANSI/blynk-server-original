package cc.blynk.common.model.messages.protocol.appllication.sharing;

import cc.blynk.common.model.messages.StringMessage;

import static cc.blynk.common.enums.Command.*;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 2/1/2015.
 */
public class GetSharedDashMessage extends StringMessage {

    public GetSharedDashMessage(int messageId, String body) {
        super(messageId, GET_SHARED_DASH, body.length(), body);
    }

    @Override
    public String toString() {
        return "GetSharedDashMessage{" + super.toString() + "}";
    }
}
