package cc.blynk.utils.structure;

import cc.blynk.utils.ParseUtil;

/**
 *
 * FIFO limited array.
 *
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 07.09.16.
 */
public class TableLimitedQueue<T> extends LimitedQueue<T> {

    private static final int POOL_SIZE = ParseUtil.parseInt(System.getProperty("table.rows.pool.size", "256"));

    public TableLimitedQueue() {
        super(POOL_SIZE);
    }

}
