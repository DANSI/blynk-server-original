package cc.blynk.server.core.protocol.exceptions;

import static cc.blynk.server.core.protocol.enums.Response.NOTIFICATION_INVALID_BODY;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 2/3/2015.
 */
public class NotificationBodyInvalidException extends BaseServerException {

    public NotificationBodyInvalidException(int msgId) {
        super("Notification message is empty or larger than limit.", msgId, NOTIFICATION_INVALID_BODY);
    }

}
