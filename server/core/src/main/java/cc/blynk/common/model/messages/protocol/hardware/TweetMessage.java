package cc.blynk.common.model.messages.protocol.hardware;

import cc.blynk.common.model.messages.StringMessage;

import static cc.blynk.common.enums.Command.*;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 2/1/2015.
 */
public class TweetMessage extends StringMessage {

    public TweetMessage(int messageId, String body) {
        super(messageId, TWEET, body.length(), body);
    }

    @Override
    public String toString() {
        return "TweetMessage{" + super.toString() + "}";
    }
}
