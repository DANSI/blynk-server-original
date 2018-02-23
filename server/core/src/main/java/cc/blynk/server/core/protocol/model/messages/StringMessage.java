package cc.blynk.server.core.protocol.model.messages;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 2/1/2015.
 */
public class StringMessage extends MessageBase {

    public final String body;
    private final Charset charset;

    public StringMessage(int messageId, short command, String body, Charset charset) {
        super(messageId, command);
        this.body = body;
        this.charset = charset;
    }

    public StringMessage(int messageId, short command, String body) {
        this(messageId, command, body, StandardCharsets.UTF_8);
    }

    @Override
    public byte[] getBytes() {
        return body.getBytes(charset);
    }

    @Override
    public String toString() {
        return super.toString() + ", body='" + body + "'";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }

        StringMessage that = (StringMessage) o;

        return !(body != null ? !body.equals(that.body) : that.body != null);

    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (body != null ? body.hashCode() : 0);
        return result;
    }
}
