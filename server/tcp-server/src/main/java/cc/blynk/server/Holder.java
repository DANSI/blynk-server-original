package cc.blynk.server;

import cc.blynk.common.stats.GlobalStats;
import cc.blynk.common.utils.ServerProperties;
import cc.blynk.server.dao.FileManager;
import cc.blynk.server.dao.SessionsHolder;
import cc.blynk.server.dao.UserRegistry;
import cc.blynk.server.storage.StorageDao;
import cc.blynk.server.storage.reporting.average.AverageAggregator;
import cc.blynk.server.workers.notifications.NotificationsProcessor;

import static cc.blynk.server.utils.ReportingUtil.getReportingFolder;

/**
 * Just a holder for all necessary objects for server instance creation.
 *
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 28.09.15.
 */
public class Holder {

    public final TransportTypeHolder transportType;

    public final FileManager fileManager;

    public final SessionsHolder sessionsHolder;

    public final UserRegistry userRegistry;

    public final StorageDao storageDao;

    public final GlobalStats stats;

    public final ServerProperties props;

    public final NotificationsProcessor notificationsProcessor;

    public final AverageAggregator averageAggregator;

    public Holder(ServerProperties serverProperties) {
        this(serverProperties, new NotificationsProcessor(
                serverProperties.getIntProperty("notifications.queue.limit", 10000)
        ));
    }

    //needed for tests only
    public Holder(ServerProperties serverProperties, NotificationsProcessor notificationsProcessor) {
        this.props = serverProperties;

        String dataFolder = serverProperties.getProperty("data.folder");

        this.transportType = new TransportTypeHolder(serverProperties);
        this.fileManager = new FileManager(dataFolder);
        this.sessionsHolder = new SessionsHolder();
        this.userRegistry = new UserRegistry(fileManager.deserialize());
        this.stats = new GlobalStats();
        this.averageAggregator = new AverageAggregator(getReportingFolder(dataFolder));
        this.storageDao = new StorageDao(averageAggregator, serverProperties);

        this.notificationsProcessor = notificationsProcessor;
    }
}
