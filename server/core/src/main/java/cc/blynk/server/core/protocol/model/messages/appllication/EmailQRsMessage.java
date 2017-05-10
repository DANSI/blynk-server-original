package cc.blynk.server.core.protocol.model.messages.appllication;

import cc.blynk.server.core.protocol.model.messages.StringMessage;

import static cc.blynk.server.core.protocol.enums.Command.EMAIL_QR;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 2/1/2015.
 */
public class EmailQRsMessage extends StringMessage {

    public EmailQRsMessage(int messageId, String body) {
        super(messageId, EMAIL_QR, body.length(), body);
    }

    @Override
    public String toString() {
        return "EmailQRsMessage{" + super.toString() + "}";
    }
}
