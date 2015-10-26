package cc.blynk.common.model.messages.protocol.appllication;

import cc.blynk.common.model.messages.StringMessage;

import static cc.blynk.common.enums.Command.ACTIVATE_DASHBOARD;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 2/1/2015.
 */
public class ActivateDashboardMessage extends StringMessage {

    public ActivateDashboardMessage(int messageId, String body) {
        super(messageId, ACTIVATE_DASHBOARD, body.length(), body);
    }

    @Override
    public String toString() {
        return "ActivateDashboardMessage{" + super.toString() + "}";
    }
}
