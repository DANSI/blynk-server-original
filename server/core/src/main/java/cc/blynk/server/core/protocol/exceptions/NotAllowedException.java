package cc.blynk.server.core.protocol.exceptions;

import cc.blynk.server.core.protocol.enums.Response;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 2/23/2015.
 */
public class NotAllowedException extends BaseServerException {

    public NotAllowedException(String message, int msgId) {
        super(message, msgId, Response.NOT_ALLOWED);
    }

}
