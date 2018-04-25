package cc.blynk.server.application.handlers.main.auth;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Threadsafe limit checker that resets counter once per specified period.
 *
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 08.04.18.
 */
class LimitChecker {

    private final int limit;
    private final long resetPeriodMillis;
    private final AtomicInteger counter;
    private volatile long lastResetTime;

    LimitChecker(int limit, long resetPeriodMillis) {
        this.limit = limit;
        this.resetPeriodMillis = resetPeriodMillis;
        this.counter = new AtomicInteger();
        this.lastResetTime = System.currentTimeMillis();
    }

    boolean isLimitReached() {
        var now = System.currentTimeMillis();
        if (now - lastResetTime >= resetPeriodMillis) {
            this.counter.set(0);
            lastResetTime = System.currentTimeMillis();
        }

        var val = counter.get();
        if (val > limit) {
            return true;
        }

        counter.incrementAndGet();
        return false;
    }

}
