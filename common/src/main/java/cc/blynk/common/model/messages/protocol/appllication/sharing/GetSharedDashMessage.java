package cc.blynk.common.model.messages.protocol.appllication.sharing;

import cc.blynk.common.model.messages.Message;

import static cc.blynk.common.enums.Command.GET_SHARED_DASH;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 2/1/2015.
 */
public class GetSharedDashMessage extends Message {

    public GetSharedDashMessage(int messageId, String body) {
        super(messageId, GET_SHARED_DASH, body.length(), body);
    }

    @Override
    public String toString() {
        return "GetSharedDashMessage{" + super.toString() + "}";
    }
}
