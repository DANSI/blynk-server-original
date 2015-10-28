package cc.blynk.server.exceptions;

import cc.blynk.common.enums.Response;
import cc.blynk.common.exceptions.BaseServerException;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 2/23/2015.
 */
public class ServerException extends BaseServerException {

    public ServerException(int msgId, String message) {
        super(message, msgId, Response.SERVER_EXCEPTION);
    }

}
