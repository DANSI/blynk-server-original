package cc.blynk.server.core.protocol.exceptions;

import cc.blynk.server.core.protocol.enums.Response;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 2/3/2015.
 */
public class NotSupportedVersion extends BaseServerException {

    public NotSupportedVersion(int msgId) {
        super("This app version is not supported anymore.", msgId, Response.NOT_SUPPORTED_VERSION);
    }

}
