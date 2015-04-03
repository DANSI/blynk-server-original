package cc.blynk.common.model.messages.protocol.hardware;

import cc.blynk.common.model.messages.Message;

import static cc.blynk.common.enums.Command.EMAIL;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 2/1/2015.
 */
public class EmailMessage extends Message {

    public EmailMessage(int messageId, String body) {
        super(messageId, EMAIL, body.length(), body);
    }

    @Override
    public String toString() {
        return "EmailMessage{" + super.toString() + "}";
    }
}
