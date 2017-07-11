package cc.blynk.server.core.protocol.model.messages.appllication;

import cc.blynk.server.core.protocol.model.messages.StringMessage;

import static cc.blynk.server.core.protocol.enums.Command.GET_ENHANCED_GRAPH_DATA;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 2/1/2015.
 */
public class GetEnhancedGraphDataStringMessage extends StringMessage {

    public GetEnhancedGraphDataStringMessage(int messageId, String body) {
        super(messageId, GET_ENHANCED_GRAPH_DATA, body.length(), body);
    }

    @Override
    public String toString() {
        return "GetEnhancedGraphDataStringMessage{" + super.toString() + "}";
    }
}
