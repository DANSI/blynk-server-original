package cc.blynk.server.exceptions;

import cc.blynk.common.enums.Response;
import cc.blynk.common.exceptions.BaseServerException;

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
