package cc.blynk.server.core.model.widgets.ui.reporting;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 31/05/2018.
 *
 */
public class ReportScheduler extends ScheduledThreadPoolExecutor {

    public final ConcurrentHashMap<Runnable, ScheduledFuture<?>> map;
    public final String downloadUrl;

    public ReportScheduler(int corePoolSize, String downloadUrl) {
        super(corePoolSize);
        setRemoveOnCancelPolicy(true);
        setExecuteExistingDelayedTasksAfterShutdownPolicy(false);
        this.map = new ConcurrentHashMap<>();
        this.downloadUrl = downloadUrl;
    }

    @Override
    public ScheduledFuture<?> schedule(Runnable task, long delay, TimeUnit unit) {
        ScheduledFuture<?> scheduledFuture = super.schedule(task, delay, unit);
        map.put(task, scheduledFuture);
        return scheduledFuture;
    }

    public boolean cancelStoredFuture(Runnable task) {
        ScheduledFuture<?> scheduledFuture = map.remove(task);
        if (scheduledFuture == null) {
            return false;
        }
        return scheduledFuture.cancel(true);
    }
}
