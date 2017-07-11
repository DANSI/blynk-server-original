package cc.blynk.server.core.protocol.model.messages.appllication;

import cc.blynk.server.core.protocol.model.messages.BinaryMessage;

import static cc.blynk.server.core.protocol.enums.Command.GET_ENHANCED_GRAPH_DATA;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 2/1/2015.
 */
public class GetEnhancedGraphDataBinaryMessage extends BinaryMessage {

    public GetEnhancedGraphDataBinaryMessage(int messageId, byte[] data) {
        super(messageId, GET_ENHANCED_GRAPH_DATA, data);
    }

    @Override
    public String toString() {
        return "GetEnhancedGraphDataBinaryMessage{" + super.toString() + "}";
    }
}
