package cc.blynk.server.core.stats.model;

import cc.blynk.server.core.BlockingIOProcessor;
import cc.blynk.server.core.model.widgets.ui.reporting.ReportScheduler;

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

    private final int reportingActiveTasks;

    private final long reportingExecutedTasks;

    private final int getServerActiveTasks;

    private final long getServerExecutedTasks;

    private final int reportsActive;

    private final long reportsExecuted;

    private final int reportsFutureMapSize;

    BlockingIOStat(BlockingIOProcessor blockingIOProcessor, ReportScheduler reportScheduler) {
        this(blockingIOProcessor.messagingExecutor.getQueue().size(),
             blockingIOProcessor.messagingExecutor.getCompletedTaskCount(),

             blockingIOProcessor.historyExecutor.getQueue().size(),
             blockingIOProcessor.historyExecutor.getCompletedTaskCount(),

             blockingIOProcessor.dbExecutor.getQueue().size(),
             blockingIOProcessor.dbExecutor.getCompletedTaskCount(),

             blockingIOProcessor.dbReportingExecutor.getQueue().size(),
             blockingIOProcessor.dbReportingExecutor.getCompletedTaskCount(),

             blockingIOProcessor.dbGetServerExecutor.getQueue().size(),
             blockingIOProcessor.dbGetServerExecutor.getCompletedTaskCount(),

             reportScheduler.getQueue().size(),
             reportScheduler.getCompletedTaskCount(),
             reportScheduler.map.size()
        );
    }

    private BlockingIOStat(int messagingActiveTasks, long messagingExecutedTasks,
                          int historyActiveTasks, long historyExecutedTasks,
                          int dbActiveTasks, long dbExecutedTasks,
                          int reportingActiveTasks, long reportingExecutedTasks,
                          int getServerActiveTasks, long getServerExecutedTasks,
                          int reportsActive, long reportsExecuted, int reportsFutureMapSize) {
        this.messagingActiveTasks = messagingActiveTasks;
        this.messagingExecutedTasks = messagingExecutedTasks;
        this.historyActiveTasks = historyActiveTasks;
        this.historyExecutedTasks = historyExecutedTasks;
        this.dbActiveTasks = dbActiveTasks;
        this.dbExecutedTasks = dbExecutedTasks;
        this.reportingActiveTasks = reportingActiveTasks;
        this.reportingExecutedTasks = reportingExecutedTasks;
        this.getServerActiveTasks = getServerActiveTasks;
        this.getServerExecutedTasks = getServerExecutedTasks;
        this.reportsActive = reportsActive;
        this.reportsExecuted = reportsExecuted;
        this.reportsFutureMapSize = reportsFutureMapSize;
    }
}
