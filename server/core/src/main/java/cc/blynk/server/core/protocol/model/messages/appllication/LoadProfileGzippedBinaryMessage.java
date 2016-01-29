package cc.blynk.server.core.protocol.model.messages.appllication;

import cc.blynk.server.core.protocol.model.messages.BinaryMessage;
import cc.blynk.utils.ByteUtils;

import static cc.blynk.server.core.protocol.enums.Command.*;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 2/1/2015.
 */
public class LoadProfileGzippedBinaryMessage extends BinaryMessage {

    public LoadProfileGzippedBinaryMessage(int messageId, byte[] data) {
        super(messageId, LOAD_PROFILE_GZIPPED, data);
    }

    public LoadProfileGzippedBinaryMessage(int messageId, String data) {
        super(messageId, LOAD_PROFILE_GZIPPED, ByteUtils.compress(data, messageId));
    }

    @Override
    public String toString() {
        return "LoadProfileGzippedBinaryMessage{" + super.toString() + "}";
    }
}
