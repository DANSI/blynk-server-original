package cc.blynk.server.core.protocol.model.messages.web;

import cc.blynk.server.core.protocol.model.messages.StringMessage;

import static cc.blynk.server.core.protocol.enums.Command.WEBAPP_HARDWARE;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 24.02.18.
 */
public class WebAppHardwareMessage extends StringMessage {

    public WebAppHardwareMessage(int messageId, String body) {
        super(messageId, WEBAPP_HARDWARE, body);
    }

    @Override
    public String toString() {
        return "WebAppHardwareMessage{" + super.toString() + "}";
    }

}
