package cc.blynk.utils.structure;

public class MapLimitedQueue<T> extends BaseLimitedQueue<T> {

    public static final int POOL_SIZE = Integer.parseInt(System.getProperty("map.strings.pool.size", "25"));

    public MapLimitedQueue() {
        super(POOL_SIZE);
    }

}
