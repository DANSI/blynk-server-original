package cc.blynk.utils.structure;

import java.util.concurrent.LinkedBlockingQueue;

/**
 *
 * FIFO limited array.
 *
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 07.09.16.
 */
public class BaseLimitedQueue<T> extends LinkedBlockingQueue<T> {

    private final int limit;

    BaseLimitedQueue(int limit) {
        super(limit);
        this.limit = limit;
    }

    @Override
    public boolean add(T o) {
        if (size() > limit - 1) {
            super.poll();
        }
        super.add(o);
        return true;
    }

}
