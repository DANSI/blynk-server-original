package cc.blynk.server.core.protocol.model.messages;

import cc.blynk.server.core.protocol.enums.Command;
import cc.blynk.server.core.protocol.enums.Response;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 2/1/2015.
 */
public class ResponseMessage extends MessageBase {

    public final int code;

    public ResponseMessage(int messageId, int responseCode) {
        super(messageId, Command.RESPONSE);
        this.code = responseCode;
    }

    @Override
    public byte[] getBytes() {
        return null;
    }

    @Override
    public String toString() {
        return "ResponseMessage{id=" + id
                + ", command=" + Command.getNameByValue(command)
                + ", responseCode=" + Response.getNameByValue(code) + "}";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ResponseMessage)) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }

        ResponseMessage that = (ResponseMessage) o;

        return code == that.code;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + code;
        return result;
    }
}
