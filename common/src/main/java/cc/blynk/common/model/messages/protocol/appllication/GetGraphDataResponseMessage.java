package cc.blynk.common.model.messages.protocol.appllication;

import cc.blynk.common.model.messages.BinaryMessage;

import static cc.blynk.common.enums.Command.GET_GRAPH_DATA_RESPONSE;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 2/1/2015.
 */
public class GetGraphDataResponseMessage extends BinaryMessage {

    public GetGraphDataResponseMessage(int messageId, byte[] data) {
        super(messageId, GET_GRAPH_DATA_RESPONSE, data);
    }

    @Override
    public String toString() {
        return "GetGraphDataResponseMessage{" + super.toString() + "}";
    }
}
