package cc.blynk.server.hardware.handlers.hardware.logic;

import cc.blynk.server.core.protocol.exceptions.QuotaLimitException;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 29.07.15.
 */
public abstract class NotificationBase {

    private final long NOTIFICATION_QUOTA_LIMIT;
    private long lastSentTs;

    NotificationBase(long defaultNotificationQuotaLimit) {
        this.NOTIFICATION_QUOTA_LIMIT = defaultNotificationQuotaLimit;
    }

    void checkIfNotificationQuotaLimitIsNotReached() {
        long currentTs = System.currentTimeMillis();
        long timePassedSinceLastMessage = (currentTs - lastSentTs);
        if (timePassedSinceLastMessage < NOTIFICATION_QUOTA_LIMIT) {
            throw new QuotaLimitException("Only 1 notification per " + NOTIFICATION_QUOTA_LIMIT + " milliseconds is allowed");
        }
        this.lastSentTs = currentTs;
    }

}
