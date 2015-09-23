package cc.blynk.common.model.messages.protocol.appllication.sharing;

import cc.blynk.common.model.messages.Message;

import static cc.blynk.common.enums.Command.REFRESH_SHARE_TOKEN;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 2/1/2015.
 */
public class RefreshShareTokenMessage extends Message {

    public RefreshShareTokenMessage(int messageId, String body) {
        super(messageId, REFRESH_SHARE_TOKEN, body.length(), body);
    }

    @Override
    public String toString() {
        return "RefreshShareTokenMessage{" + super.toString() + "}";
    }
}
