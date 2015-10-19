package cc.blynk.common.model.messages.protocol.appllication;

import cc.blynk.common.model.messages.Message;

import static cc.blynk.common.enums.Command.SAVE_DASH;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 2/1/2015.
 */
public class SaveDashMessage extends Message {

    public SaveDashMessage(int messageId, String body) {
        super(messageId, SAVE_DASH, body.length(), body);
    }

    @Override
    public String toString() {
        return "SaveDashMessage{" + super.toString() + "}";
    }
}
