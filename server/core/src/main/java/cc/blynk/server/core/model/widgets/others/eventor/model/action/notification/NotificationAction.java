package cc.blynk.server.core.model.widgets.others.eventor.model.action.notification;

import cc.blynk.server.core.model.widgets.others.eventor.model.action.BaseAction;
import cc.blynk.server.core.protocol.model.messages.StringMessage;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 23.08.16.
 */
public abstract class NotificationAction extends BaseAction {

    public String message;

    public abstract StringMessage makeMessage(String triggerValue);

}
