package cc.blynk.common.model.messages.protocol.appllication;

import cc.blynk.common.model.messages.Message;

import static cc.blynk.common.enums.Command.GET_GRAPH_DATA_RESPONSE;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 2/1/2015.
 */
public class GetGraphDataResponseMessage extends Message {

    public byte[] data;

    public GetGraphDataResponseMessage(int messageId, byte[] data) {
        super(messageId, GET_GRAPH_DATA_RESPONSE, data.length, null);
        this.data = data;
    }

    @Override
    public byte[] getBytes() {
        return data;
    }

    @Override
    public String toString() {
        return "GetGraphDataResponseMessage{" + super.toString() + "}";
    }
}
