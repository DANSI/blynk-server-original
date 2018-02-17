package cc.blynk.server.core.stats.model;

import cc.blynk.server.core.BlockingIOProcessor;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 03.05.17.
 */
class BlockingIOStat {

    private final int messagingActiveTasks;

    private final long messagingExecutedTasks;

    private final int historyActiveTasks;

    private final long historyExecutedTasks;

    private final int dbActiveTasks;

    private final long dbExecutedTasks;

    private final int getServerActiveTasks;

    private final long getServerExecutedTasks;

    BlockingIOStat(BlockingIOProcessor blockingIOProcessor) {
        this(blockingIOProcessor.messagingExecutor.getQueue().size(),
             blockingIOProcessor.messagingExecutor.getCompletedTaskCount(),

             blockingIOProcessor.historyExecutor.getQueue().size(),
             blockingIOProcessor.historyExecutor.getCompletedTaskCount(),

             blockingIOProcessor.dbExecutor.getQueue().size(),
             blockingIOProcessor.dbExecutor.getCompletedTaskCount(),

             blockingIOProcessor.dbGetServerExecutor.getQueue().size(),
             blockingIOProcessor.dbGetServerExecutor.getCompletedTaskCount()
        );
    }

    private BlockingIOStat(int messagingActiveTasks, long messagingExecutedTasks,
                          int historyActiveTasks, long historyExecutedTasks,
                          int dbActiveTasks, long dbExecutedTasks,
                          int getServerActiveTasks, long getServerExecutedTasks) {
        this.messagingActiveTasks = messagingActiveTasks;
        this.messagingExecutedTasks = messagingExecutedTasks;
        this.historyActiveTasks = historyActiveTasks;
        this.historyExecutedTasks = historyExecutedTasks;
        this.dbActiveTasks = dbActiveTasks;
        this.dbExecutedTasks = dbExecutedTasks;
        this.getServerActiveTasks = getServerActiveTasks;
        this.getServerExecutedTasks = getServerExecutedTasks;
    }
}
