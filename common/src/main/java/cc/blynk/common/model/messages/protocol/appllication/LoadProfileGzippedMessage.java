package cc.blynk.common.model.messages.protocol.appllication;

import cc.blynk.common.model.messages.BinaryMessage;

import static cc.blynk.common.enums.Command.LOAD_PROFILE_GZIPPED;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 2/1/2015.
 */
public class LoadProfileGzippedMessage extends BinaryMessage {

    public LoadProfileGzippedMessage(int messageId, byte[] data) {
        super(messageId, LOAD_PROFILE_GZIPPED, data);
    }

    @Override
    public String toString() {
        return "LoadProfileGzippedMessage{" + super.toString() + "}";
    }
}
