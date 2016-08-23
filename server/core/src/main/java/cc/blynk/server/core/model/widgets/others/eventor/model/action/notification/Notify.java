package cc.blynk.server.core.model.widgets.others.eventor.model.action.notification;

import cc.blynk.server.core.protocol.model.messages.StringMessage;
import cc.blynk.server.core.protocol.model.messages.hardware.PushMessage;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 01.08.16.
 */
public class Notify extends NotificationAction {

    public Notify() {
    }

    public Notify(String message) {
        this.message = message;
    }

    @Override
    StringMessage makeMessage(String triggerValue) {
        return new PushMessage(888, format(message, triggerValue));
    }
}
