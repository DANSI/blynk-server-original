package cc.blynk.server.exceptions;

import cc.blynk.common.enums.Response;
import cc.blynk.common.exceptions.BaseServerException;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 2/3/2015.
 */
public class ServerBusyException extends BaseServerException {

    public ServerBusyException(int msgId) {
        super("Queue is full. Could not process further.", msgId, Response.SERVER_BUSY_EXCEPTION);
    }

}
