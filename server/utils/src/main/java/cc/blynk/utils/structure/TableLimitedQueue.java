package cc.blynk.utils.structure;

/**
 *
 * FIFO limited array.
 *
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 07.09.16.
 */
public class TableLimitedQueue<T> extends BaseLimitedQueue<T> {

    private static final int TABLE_POOL_SIZE = Integer.parseInt(System.getProperty("table.rows.pool.size", "100"));

    public TableLimitedQueue() {
        super(TABLE_POOL_SIZE);
    }

}
