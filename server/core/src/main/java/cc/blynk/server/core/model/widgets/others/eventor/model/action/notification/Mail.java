package cc.blynk.server.core.model.widgets.others.eventor.model.action.notification;

import cc.blynk.server.core.protocol.model.messages.StringMessage;
import cc.blynk.server.core.protocol.model.messages.hardware.MailMessage;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 01.08.16.
 */
public class Mail extends NotificationAction {

    public Mail() {
    }

    public Mail(String message) {
        this.message = message;
    }

    @Override
    public StringMessage makeMessage(String triggerValue) {
        return new MailMessage(888, format(message, triggerValue));
    }
}
