package cc.blynk.server.core.protocol.model.messages.appllication;

import cc.blynk.server.core.protocol.model.messages.StringMessage;

import static cc.blynk.server.core.protocol.enums.Command.OUTDATED_APP_NOTIFICATION;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 2/1/2015.
 */
public class AppIsOutdatedNotification extends StringMessage {

    public AppIsOutdatedNotification(int messageId, String body) {
        super(messageId, OUTDATED_APP_NOTIFICATION, body.length(), body);
    }

    @Override
    public String toString() {
        return "AppIsOutdatedNotification{" + super.toString() + "}";
    }
}
