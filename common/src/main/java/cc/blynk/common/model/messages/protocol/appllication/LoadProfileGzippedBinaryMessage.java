package cc.blynk.common.model.messages.protocol.appllication;

import cc.blynk.common.model.messages.BinaryMessage;

import static cc.blynk.common.enums.Command.LOAD_PROFILE_GZIPPED;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 2/1/2015.
 */
public class LoadProfileGzippedBinaryMessage extends BinaryMessage {

    public LoadProfileGzippedBinaryMessage(int messageId, byte[] data) {
        super(messageId, LOAD_PROFILE_GZIPPED, data);
    }

    @Override
    public String toString() {
        return "LoadProfileGzippedBinaryMessage{" + super.toString() + "}";
    }
}
