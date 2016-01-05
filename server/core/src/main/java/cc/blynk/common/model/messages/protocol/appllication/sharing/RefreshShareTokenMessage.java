package cc.blynk.common.model.messages.protocol.appllication.sharing;

import cc.blynk.common.model.messages.StringMessage;

import static cc.blynk.common.enums.Command.*;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 2/1/2015.
 */
public class RefreshShareTokenMessage extends StringMessage {

    public RefreshShareTokenMessage(int messageId, String body) {
        super(messageId, REFRESH_SHARE_TOKEN, body.length(), body);
    }

    @Override
    public String toString() {
        return "RefreshShareTokenMessage{" + super.toString() + "}";
    }
}
