package cc.blynk.common.model.messages.protocol;

import cc.blynk.common.model.messages.Message;
import cc.blynk.common.utils.StringUtils;

import static cc.blynk.common.enums.Command.HARDWARE_COMMAND;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 2/1/2015.
 */
public class HardwareMessage extends Message {

    public HardwareMessage(int messageId, String body) {
        super(messageId, HARDWARE_COMMAND, body.length(), body);
    }

    public static String attachTS(String body) {
        if (body.charAt(body.length() - 1) == StringUtils.BODY_SEPARATOR) {
            return body + System.currentTimeMillis();
        } else {
            return body + StringUtils.BODY_SEPARATOR + System.currentTimeMillis();
        }
    }

    public HardwareMessage updateMessageBody(String newBody) {
        this.body = newBody;
        this.length = newBody.length();
        return this;
    }

    @Override
    public String toString() {
        return "HardwareMessage{" + super.toString() + "}";
    }
}
