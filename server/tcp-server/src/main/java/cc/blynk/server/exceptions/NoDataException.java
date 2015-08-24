package cc.blynk.server.exceptions;

import cc.blynk.common.enums.Response;
import cc.blynk.common.exceptions.BaseServerException;

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
