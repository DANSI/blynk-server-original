package cc.blynk.server.exceptions;

import cc.blynk.common.enums.Response;
import cc.blynk.common.exceptions.BaseServerException;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 2/3/2015.
 */
public class TweetBodyInvalidException extends BaseServerException {

    public TweetBodyInvalidException(int msgId) {
        super("Tweet message is empty or larger 140 chars.", msgId, Response.TWEET_BODY_INVALID_EXCEPTION);
    }

}
