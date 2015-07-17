package cc.blynk.server.exceptions;

import cc.blynk.common.enums.Response;
import cc.blynk.common.exceptions.BaseServerException;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 2/3/2015.
 */
public class IllegalCommandBodyException extends BaseServerException {

    public IllegalCommandBodyException(String message, int msgId) {
        super(message, msgId, Response.ILLEGAL_COMMAND_BODY);
    }

}
