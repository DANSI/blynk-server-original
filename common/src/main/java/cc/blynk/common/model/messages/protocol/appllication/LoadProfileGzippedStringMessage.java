package cc.blynk.common.model.messages.protocol.appllication;

import cc.blynk.common.model.messages.StringMessage;

import static cc.blynk.common.enums.Command.LOAD_PROFILE_GZIPPED;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 2/1/2015.
 */
public class LoadProfileGzippedStringMessage extends StringMessage {

    public LoadProfileGzippedStringMessage(int messageId, String body) {
        super(messageId, LOAD_PROFILE_GZIPPED, body.length(), body);
    }

    @Override
    public String toString() {
        return "LoadProfileGzippedStringMessage{" + super.toString() + "}";
    }
}
