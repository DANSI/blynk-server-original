package cc.blynk.server.core.protocol.model.messages.appllication;

import cc.blynk.server.core.protocol.model.messages.StringMessage;

import static cc.blynk.server.core.protocol.enums.Command.UPDATE_PROJECT_SETTINGS;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 2/1/2015.
 */
public class UpdateDashSettingsMessage extends StringMessage {

    public UpdateDashSettingsMessage(int messageId, String body) {
        super(messageId, UPDATE_PROJECT_SETTINGS, body.length(), body);
    }

    @Override
    public String toString() {
        return "UpdateProjectSetting{" + super.toString() + "}";
    }
}
