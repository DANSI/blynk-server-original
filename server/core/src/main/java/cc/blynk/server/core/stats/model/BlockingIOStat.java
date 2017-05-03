package cc.blynk.server.core.stats.model;

import cc.blynk.server.core.BlockingIOProcessor;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 03.05.17.
 */
public class BlockingIOStat {

    private final int activeTasks;

    private final long executedTasks;

    public BlockingIOStat(BlockingIOProcessor blockingIOProcessor) {
        this(blockingIOProcessor.messagingExecutorTasksQueue(), blockingIOProcessor.messagingExecutorTasksExecuted());
    }

    public BlockingIOStat(int activeTasks, long executedTasks) {
        this.activeTasks = activeTasks;
        this.executedTasks = executedTasks;
    }
}
