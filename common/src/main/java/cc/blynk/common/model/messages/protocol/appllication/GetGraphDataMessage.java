package cc.blynk.common.model.messages.protocol.appllication;

import cc.blynk.common.model.messages.Message;

import static cc.blynk.common.enums.Command.GET_GRAPH_DATA;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 2/1/2015.
 */
public class GetGraphDataMessage extends Message {

    public GetGraphDataMessage(int messageId, String body) {
        super(messageId, GET_GRAPH_DATA, body.length(), body);
    }

    @Override
    public String toString() {
        return "GetGraphDataMessage{" + super.toString() + "}";
    }
}
