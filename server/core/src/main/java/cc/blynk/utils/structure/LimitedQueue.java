package cc.blynk.utils.structure;

import java.util.LinkedList;

/**
 * FIFO limited queue. Used to store last terminal messages.
 *
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 24.05.16.
 */
public class LimitedQueue<T> extends LinkedList<T> {

    private final int limit;

    public LimitedQueue(int limit) {
        this.limit = limit;
    }

    @Override
    public boolean add(T o) {
        super.add(o);
        if (size() > limit) {
            super.remove();
        }
        return true;
    }

    public void order(int oldIndex, int newIndex) {
        T e = remove(oldIndex);
        add(newIndex, e);
    }

    public T get(int index) {
        int start = 0;
        for (T t : this) {
            if (start == index) {
                return t;
            }
            start++;
        }
        return null;
    }

}
