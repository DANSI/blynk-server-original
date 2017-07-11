package cc.blynk.server.core.protocol.model.messages.appllication;

import cc.blynk.server.core.protocol.model.messages.StringMessage;

import static cc.blynk.server.core.protocol.enums.Command.DELETE_ENHANCED_GRAPH_DATA;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 2/1/2015.
 */
public class DeleteEnhancedGraphDataStringMessage extends StringMessage {

    public DeleteEnhancedGraphDataStringMessage(int messageId, String body) {
        super(messageId, DELETE_ENHANCED_GRAPH_DATA, body.length(), body);
    }

    @Override
    public String toString() {
        return "DeleteEnhancedGraphDataStringMessage{" + super.toString() + "}";
    }
}
