package cc.blynk.server.core.processors;

import cc.blynk.server.core.protocol.exceptions.QuotaLimitException;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 29.07.15.
 */
public abstract class NotificationBase {

    private final long notificationQuotaLimit;
    private long lastSentTs;
    public final static QuotaLimitException EXCEPTION_CACHE = new QuotaLimitException("Notification limit reached.");

    public NotificationBase(long defaultNotificationQuotaLimit) {
        this.notificationQuotaLimit = defaultNotificationQuotaLimit;
    }

    protected void checkIfNotificationQuotaLimitIsNotReached() {
        checkIfNotificationQuotaLimitIsNotReached(System.currentTimeMillis());
    }

    protected void checkIfNotificationQuotaLimitIsNotReached(final long currentTs) {
        final long timePassedSinceLastMessage = (currentTs - lastSentTs);
        if (timePassedSinceLastMessage < notificationQuotaLimit) {
            throw EXCEPTION_CACHE;
        }
        this.lastSentTs = currentTs;
    }

}
