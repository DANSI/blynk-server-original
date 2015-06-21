package cc.blynk.server.notifications.twitter.exceptions;

import cc.blynk.common.enums.Response;
import cc.blynk.common.exceptions.BaseServerException;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 2/3/2015.
 */
public class NotificationNotAuthorizedException extends BaseServerException {

    public NotificationNotAuthorizedException(String message, int msgId) {
        super(message, msgId, Response.NOTIFICATION_NOT_AUTHORIZED_EXCEPTION);
    }

}
