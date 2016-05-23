package cc.blynk.utils;

import java.util.LinkedList;

/**
 * FIFO limited queue. Used to store last terminal messages.
 *
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 24.05.16.
 */
public class LimitedQueue<E> extends LinkedList<E> {

    private final int limit;

    public LimitedQueue(int limit) {
        this.limit = limit;
    }

    @Override
    public boolean add(E o) {
        super.add(o);
        if (size() > limit) {
            super.remove();
        }
        return true;
    }
}
