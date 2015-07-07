package cc.blynk.server.exceptions;

import cc.blynk.common.enums.Response;
import cc.blynk.common.exceptions.BaseServerException;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 2/3/2015.
 */
public class GetGraphDataException extends BaseServerException {

    public GetGraphDataException(int msgId) {
        super("Server exception!", msgId, Response.GET_GRAPH_DATA_EXCEPTION);
    }

}
