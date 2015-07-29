package cc.blynk.server.handlers.hardware.logic;

import cc.blynk.server.exceptions.QuotaLimitException;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 29.07.15.
 */
public abstract class NotificationBase {

    private final long defaultNotificationQuotaLimit;

    NotificationBase(long defaultNotificationQuotaLimit) {
        this.defaultNotificationQuotaLimit = defaultNotificationQuotaLimit;
    }

    long checkIfNotificationQuotaLimitIsNotReached(long lastAccessTime, int msgId) {
        long currentTs = System.currentTimeMillis();
        long timePassedSinceLastMessage = (currentTs - lastAccessTime);
        if (timePassedSinceLastMessage < defaultNotificationQuotaLimit) {
            throw new QuotaLimitException(String.format("Only 1 notification per %s miliseconds is allowed", defaultNotificationQuotaLimit), msgId);
        }
        return currentTs;
    }

}
