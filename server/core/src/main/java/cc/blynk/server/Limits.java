package cc.blynk.server;

import cc.blynk.utils.FileLoaderUtil;
import cc.blynk.utils.properties.ServerProperties;

/**
 * This is helper class for holding all user limits.
 * It is created for dependency injection mostly.
 *
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 07.01.17.
 */
public class Limits {

    public final int webRequestMaxSize;

    //user limits
    public final int deviceLimit;
    public final int tagsLimit;
    public final int dashboardsLimit;
    public final int widgetSizeLimitBytes;
    public final int profileSizeLimitBytes;

    //hardware side limits
    public final long notificationPeriodLimitSec;
    public final int userQuotaLimit;
    public final long webhookPeriodLimitation;
    public final int webhookResponseSuzeLimitBytes;
    public final int webhookFailureLimit;
    public final int hardwareIdleTimeout;

    //texts
    public volatile String tokenBody;
    public final String dynamicMailBody;
    public final String staticMailBody;

    Limits(ServerProperties props) {
        this.webRequestMaxSize = props.getIntProperty("web.request.max.size", 512 * 1024);

        this.deviceLimit = props.getIntProperty("user.devices.limit", 25);
        this.tagsLimit = props.getIntProperty("user.tags.limit", 100);
        this.dashboardsLimit = props.getIntProperty("user.dashboard.max.limit", 100);
        this.widgetSizeLimitBytes = props.getIntProperty("user.widget.max.size.limit", 10) * 1024;
        this.profileSizeLimitBytes = props.getIntProperty("user.profile.max.size", 64) * 1024;

        this.notificationPeriodLimitSec =
                props.getLongProperty("notifications.frequency.user.quota.limit", 15L) * 1000L;
        this.userQuotaLimit = props.getIntProperty("user.message.quota.limit", 100);
        this.webhookPeriodLimitation =
                isUnlimited(props.getLongProperty("webhooks.frequency.user.quota.limit", 1000), -1L);
        this.webhookResponseSuzeLimitBytes = props.getIntProperty("webhooks.response.size.limit", 64) * 1024;
        this.webhookFailureLimit =
                isUnlimited(props.getIntProperty("webhooks.failure.count.limit", 10), Integer.MAX_VALUE);
        this.hardwareIdleTimeout = props.getIntProperty("hard.socket.idle.timeout", 0);

        this.tokenBody = FileLoaderUtil.readTokenMailBody();
        this.dynamicMailBody = FileLoaderUtil.readDynamicMailBody();
        this.staticMailBody = FileLoaderUtil.readStaticMailBody();
    }

    private static int isUnlimited(int val, int max) {
        if (val == 0) {
            return max;
        }
        return val;
    }

    private static long isUnlimited(long val, long max) {
        if (val == 0) {
            return max;
        }
        return val;
    }

}
