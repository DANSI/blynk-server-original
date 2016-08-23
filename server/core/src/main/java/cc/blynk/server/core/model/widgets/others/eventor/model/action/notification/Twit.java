package cc.blynk.server.core.model.widgets.others.eventor.model.action.notification;

import cc.blynk.server.core.protocol.model.messages.StringMessage;
import cc.blynk.server.core.protocol.model.messages.hardware.TwitMessage;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 01.08.16.
 */
public class Twit extends NotificationAction {

    public Twit() {
    }

    public Twit(String message) {
        this.message = message;
    }

    @Override
    public StringMessage makeMessage(String triggerValue) {
        return new TwitMessage(888, format(message, triggerValue));
    }

}
