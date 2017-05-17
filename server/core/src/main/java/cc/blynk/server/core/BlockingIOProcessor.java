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

    //pool for messaging
    private final ThreadPoolExecutor messagingExecutor;

    //DB pool is needed as in case DB goes down messaging still should work
    private final ThreadPoolExecutor dbExecutor;

    //separate pool for history graph data
    private final ThreadPoolExecutor historyExecutor;

    public BlockingIOProcessor(int poolSize, int maxQueueSize) {
        this.messagingExecutor = new ThreadPoolExecutor(
                poolSize / 4, poolSize / 3,
                2L, TimeUnit.MINUTES,
                new ArrayBlockingQueue<>(maxQueueSize)
        );

        this.dbExecutor = new ThreadPoolExecutor(poolSize / 3, poolSize / 2, 2L, TimeUnit.MINUTES, new ArrayBlockingQueue<>(250));
        //local server doesn't use DB usually, so this thread may be not necessary
        this.dbExecutor.allowCoreThreadTimeOut(true);

        this.historyExecutor = new ThreadPoolExecutor(poolSize / 2, poolSize, 2L, TimeUnit.MINUTES, new ArrayBlockingQueue<>(250));
    }

    public void execute(Runnable task) {
        messagingExecutor.execute(task);
    }

    public void executeDB(Runnable task) {
        dbExecutor.execute(task);
    }

    public void executeHistory(Runnable task) {
        historyExecutor.execute(task);
    }

    @Override
    public void close() {
        dbExecutor.shutdown();
        messagingExecutor.shutdown();
        historyExecutor.shutdown();
    }

    public int getActiveCount() {
        return messagingExecutor.getActiveCount();
    }



    public int messagingActiveTasks() {
        return messagingExecutor.getQueue().size();
    }

    public long messagingExecutedTasks() {
        return messagingExecutor.getCompletedTaskCount();
    }


    public int historyActiveTasks() {
        return historyExecutor.getQueue().size();
    }

    public long historyExecutedTasks() {
        return historyExecutor.getCompletedTaskCount();
    }


    public int dbActiveTasks() {
        return dbExecutor.getQueue().size();
    }

    public long dbExecutedTasks() {
        return dbExecutor.getCompletedTaskCount();
    }
}
