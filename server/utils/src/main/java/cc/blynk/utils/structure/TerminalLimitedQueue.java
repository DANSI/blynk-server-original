package cc.blynk.utils.structure;

/**
 *
 * FIFO limited array.
 *
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 07.09.16.
 */
public class TerminalLimitedQueue<T> extends BaseLimitedQueue<T> {

    public static final int POOL_SIZE = Integer.parseInt(System.getProperty("terminal.strings.pool.size", "25"));

    public TerminalLimitedQueue() {
        super(POOL_SIZE);
    }

}
