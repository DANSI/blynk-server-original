package cc.blynk.utils.structure;

/**
 *
 * FIFO limited array.
 *
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 07.09.16.
 */
public class LCDLimitedQueue<T> extends BaseLimitedQueue<T> {

    public static final int POOL_SIZE = Integer.parseInt(System.getProperty("lcd.strings.pool.size", "6"));

    public LCDLimitedQueue() {
        super(POOL_SIZE);
    }

}
