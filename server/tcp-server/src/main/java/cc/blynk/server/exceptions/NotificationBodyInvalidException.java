package cc.blynk.server.exceptions;

import cc.blynk.common.enums.Response;
import cc.blynk.common.exceptions.BaseServerException;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 2/3/2015.
 */
public class NotificationBodyInvalidException extends BaseServerException {

    public NotificationBodyInvalidException(int msgId) {
        super("Notification message is empty or larger than limit.", msgId, Response.NOTIFICATION_INVALID_BODY_EXCEPTION);
    }

}
