package cc.blynk.common.model.messages;

import cc.blynk.common.utils.Config;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 2/1/2015.
 */
public abstract class StringMessage extends MessageBase {

    public String body;

    public StringMessage(int messageId, short command, int length, String body) {
        super(messageId, command, length);
        this.body = body;
    }

    @Override
    public byte[] getBytes() {
        return body.getBytes(Config.DEFAULT_CHARSET);
    }

    @Override
    public String toString() {
        return super.toString() + ", body='" + body + "'";
    }
}
