package cc.blynk.server.core;

import java.io.Closeable;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Wrapper around ThreadPoolExecutor that should perform blocking IO operations.
 * Due to async nature of netty performing Blocking operations withing netty pipeline
 * will cause performance issues. So Blocking operations should always
 * executed via this wrapper.
 *
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 07.04.15.
 */
public class BlockingIOProcessor implements Closeable {

    private final ThreadPoolExecutor messagingExecutor;
    //separate DB pool is needed as in case DB goes down messaging still should work
    private final ThreadPoolExecutor dbExecutor;

    public BlockingIOProcessor(int poolSize, int maxQueueSize) {
        poolSize = Math.max(3, poolSize);
        int dbPoolSize = 2;
        this.messagingExecutor = new ThreadPoolExecutor(
                poolSize - dbPoolSize, poolSize - dbPoolSize,
                0L, TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(maxQueueSize)
        );
        this.dbExecutor = new ThreadPoolExecutor(dbPoolSize, dbPoolSize, 0L, TimeUnit.MILLISECONDS, new ArrayBlockingQueue<>(500));
    }

    public void execute(Runnable task) {
        messagingExecutor.execute(task);
    }

    public void executeDB(Runnable task) {
        dbExecutor.execute(task);
    }

    @Override
    public void close() {
        dbExecutor.shutdown();
        messagingExecutor.shutdown();
    }

    public int getActiveCount() {
        return messagingExecutor.getActiveCount();
    }

}
