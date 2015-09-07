package cc.blynk.server.handlers.hardware.logic;

import cc.blynk.server.exceptions.QuotaLimitException;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 29.07.15.
 */
public abstract class NotificationBase {

    private final long NOTIFICATION_QUOTA_LIMIT;
    volatile long lastSentTs;

    NotificationBase(long defaultNotificationQuotaLimit) {
        this.NOTIFICATION_QUOTA_LIMIT = defaultNotificationQuotaLimit;
    }

    void checkIfNotificationQuotaLimitIsNotReached(int msgId) {
        long currentTs = System.currentTimeMillis();
        long timePassedSinceLastMessage = (currentTs - lastSentTs);
        if (timePassedSinceLastMessage < NOTIFICATION_QUOTA_LIMIT) {
            throw new QuotaLimitException(String.format("Only 1 notification per %s miliseconds is allowed", NOTIFICATION_QUOTA_LIMIT), msgId);
        }
        this.lastSentTs = currentTs;
    }

}
