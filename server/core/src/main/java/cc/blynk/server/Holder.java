package cc.blynk.server;

import cc.blynk.server.core.BlockingIOProcessor;
import cc.blynk.server.core.dao.FileManager;
import cc.blynk.server.core.dao.ReportingDao;
import cc.blynk.server.core.dao.SessionDao;
import cc.blynk.server.core.dao.UserDao;
import cc.blynk.server.core.reporting.average.AverageAggregator;
import cc.blynk.server.core.stats.GlobalStats;
import cc.blynk.server.db.DBManager;
import cc.blynk.server.notifications.mail.MailWrapper;
import cc.blynk.server.notifications.push.GCMWrapper;
import cc.blynk.server.notifications.twitter.TwitterWrapper;
import cc.blynk.utils.Config;
import cc.blynk.utils.FileLoaderUtil;
import cc.blynk.utils.ServerProperties;

import static cc.blynk.utils.ReportingUtil.*;

/**
 * Just a holder for all necessary objects for server instance creation.
 *
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 28.09.15.
 */
public class Holder {

    public final FileManager fileManager;

    public final SessionDao sessionDao;

    public final UserDao userDao;

    public final ReportingDao reportingDao;

    public final DBManager dbManager;

    public final GlobalStats stats;

    public final ServerProperties props;

    public final AverageAggregator averageAggregator;

    public final BlockingIOProcessor blockingIOProcessor;

    public final TwitterWrapper twitterWrapper;
    public final MailWrapper mailWrapper;
    public final GCMWrapper gcmWrapper;

    public Holder(ServerProperties serverProperties) {
        this.props = serverProperties;

        String dataFolder = serverProperties.getProperty("data.folder");

        this.fileManager = new FileManager(dataFolder);
        this.sessionDao = new SessionDao();
        this.userDao = new UserDao(fileManager.deserialize());
        this.stats = new GlobalStats();
        final String reportingFolder = getReportingFolder(dataFolder);
        this.averageAggregator = new AverageAggregator(reportingFolder);
        this.reportingDao = new ReportingDao(reportingFolder, averageAggregator, serverProperties);
        this.dbManager = new DBManager();

        this.twitterWrapper = new TwitterWrapper();
        this.mailWrapper = new MailWrapper(new ServerProperties(Config.MAIL_PROPERTIES_FILENAME));
        this.gcmWrapper = new GCMWrapper(new ServerProperties(GCMWrapper.GCM_PROPERTIES_FILENAME));

        this.blockingIOProcessor = new BlockingIOProcessor(
                serverProperties.getIntProperty("notifications.queue.limit", 10000),
                FileLoaderUtil.readFileAsString(Config.TOKEN_MAIL_BODY),
                reportingDao
        );
    }

    //for tests only
    public Holder(ServerProperties serverProperties, TwitterWrapper twitterWrapper, MailWrapper mailWrapper, GCMWrapper gcmWrapper) {
        this.props = serverProperties;

        String dataFolder = serverProperties.getProperty("data.folder");

        this.fileManager = new FileManager(dataFolder);
        this.sessionDao = new SessionDao();
        this.userDao = new UserDao(fileManager.deserialize());
        this.stats = new GlobalStats();
        final String reportingFolder = getReportingFolder(dataFolder);
        this.averageAggregator = new AverageAggregator(reportingFolder);
        this.reportingDao = new ReportingDao(reportingFolder, averageAggregator, serverProperties);
        this.dbManager = new DBManager();

        this.twitterWrapper = twitterWrapper;
        this.mailWrapper = mailWrapper;
        this.gcmWrapper = gcmWrapper;

        this.blockingIOProcessor = new BlockingIOProcessor(
                serverProperties.getIntProperty("notifications.queue.limit", 10000),
                FileLoaderUtil.readFileAsString(Config.TOKEN_MAIL_BODY),
                reportingDao
        );
    }

}
