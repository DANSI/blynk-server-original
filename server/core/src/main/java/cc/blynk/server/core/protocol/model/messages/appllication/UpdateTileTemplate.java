package cc.blynk.server.core.protocol.model.messages.appllication;

import cc.blynk.server.core.protocol.model.messages.StringMessage;

import static cc.blynk.server.core.protocol.enums.Command.UPDATE_TILE_TEMPLATE;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 2/1/2015.
 */
public class UpdateTileTemplate extends StringMessage {

    public UpdateTileTemplate(int messageId, String body) {
        super(messageId, UPDATE_TILE_TEMPLATE, body.length(), body);
    }

    @Override
    public String toString() {
        return "UpdateTileTemplate{" + super.toString() + "}";
    }
}
