package cc.blynk.server.core.protocol.exceptions;

import cc.blynk.server.core.protocol.enums.Response;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 2/3/2015.
 */
public class NoDataException extends BaseServerException {

    public NoDataException(int msgId) {
        super("No Data", msgId, Response.NO_DATA_EXCEPTION);
    }

}
