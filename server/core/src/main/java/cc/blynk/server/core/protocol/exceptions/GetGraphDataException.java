package cc.blynk.server.core.protocol.exceptions;

import cc.blynk.server.core.protocol.enums.Response;

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
