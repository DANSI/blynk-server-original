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
public class TableLimitedQueue<T> extends LinkedBlockingQueue<T> {

    private static final int TABLE_POOL_SIZE = Integer.parseInt(System.getProperty("table.rows.pool.size", "100"));

    private final int limit;

    public TableLimitedQueue() {
        this(TABLE_POOL_SIZE);
    }

    TableLimitedQueue(int limit) {
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
