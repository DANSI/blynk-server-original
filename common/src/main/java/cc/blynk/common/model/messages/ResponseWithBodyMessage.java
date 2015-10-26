package cc.blynk.common.model.messages;

import cc.blynk.common.enums.Command;
import cc.blynk.common.enums.Response;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 2/1/2015.
 */
public class ResponseWithBodyMessage extends MessageBase {

    public final int dashId;

    public ResponseWithBodyMessage(int messageId, short command, int responseCode, int dashId) {
        super(messageId, command, responseCode);
        this.dashId = dashId;
    }

    @Override
    public byte[] getBytes() {
        return null;
    }

    @Override
    public String toString() {
        return "ResponseWithBodyMessage{id=" + id +
                ", command=" + Command.getNameByValue(command) +
                ", responseCode=" + Response.getNameByValue(length) +
                ", dashId=" + dashId +
                "}";
    }
}
