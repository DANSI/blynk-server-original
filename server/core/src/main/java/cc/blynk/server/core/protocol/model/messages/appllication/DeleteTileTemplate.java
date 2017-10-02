package cc.blynk.server.core.protocol.model.messages.appllication;

import cc.blynk.server.core.protocol.model.messages.StringMessage;

import static cc.blynk.server.core.protocol.enums.Command.DELETE_TILE_TEMPLATE;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 2/1/2015.
 */
public class DeleteTileTemplate extends StringMessage {

    public DeleteTileTemplate(int messageId, String body) {
        super(messageId, DELETE_TILE_TEMPLATE, body.length(), body);
    }

    @Override
    public String toString() {
        return "DeleteTileTemplate{" + super.toString() + "}";
    }
}
