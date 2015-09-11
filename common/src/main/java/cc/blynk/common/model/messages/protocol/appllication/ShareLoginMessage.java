package cc.blynk.common.model.messages.protocol.appllication;

import cc.blynk.common.model.messages.Message;

import static cc.blynk.common.enums.Command.SHARE_LOGIN;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 2/1/2015.
 */
public class ShareLoginMessage extends Message {

    public ShareLoginMessage(int messageId, String body) {
        super(messageId, SHARE_LOGIN, body.length(), body);
    }

    @Override
    public String toString() {
        return "ShareLoginMessage{" + super.toString() + "}";
    }
}
