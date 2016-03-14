package cc.blynk.server.core.protocol.model.messages.appllication;

import cc.blynk.server.core.protocol.model.messages.StringMessage;

import static cc.blynk.server.core.protocol.enums.Command.*;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 2/1/2015.
 */
public class GetEnergy extends StringMessage {

    public GetEnergy(int messageId) {
        super(messageId, GET_ENERGY, 0, "");
    }

    @Override
    public String toString() {
        return "GetEnergy{" + super.toString() + "}";
    }
}
