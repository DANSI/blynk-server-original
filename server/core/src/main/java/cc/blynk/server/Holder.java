package cc.blynk.server;

import cc.blynk.server.core.BlockingIOProcessor;
import cc.blynk.server.core.dao.FileManager;
import cc.blynk.server.core.dao.ReportingDao;
import cc.blynk.server.core.dao.SessionDao;
import cc.blynk.server.core.dao.TokenManager;
import cc.blynk.server.core.dao.UserDao;
import cc.blynk.server.core.processors.EventorProcessor;
import cc.blynk.server.core.reporting.average.AverageAggregator;
import cc.blynk.server.core.stats.GlobalStats;
import cc.blynk.server.db.DBManager;
import cc.blynk.server.notifications.mail.MailWrapper;
import cc.blynk.server.notifications.push.GCMWrapper;
import cc.blynk.server.notifications.sms.SMSWrapper;
import cc.blynk.server.notifications.twitter.TwitterWrapper;
import cc.blynk.server.redis.RedisClient;
import cc.blynk.server.workers.timer.TimerWorker;
import cc.blynk.utils.FileLoaderUtil;
import cc.blynk.utils.IPUtils;
import cc.blynk.utils.ServerProperties;
import org.asynchttpclient.DefaultAsyncHttpClient;
import org.asynchttpclient.DefaultAsyncHttpClientConfig;

import java.io.Closeable;

import static cc.blynk.utils.ReportingUtil.getReportingFolder;

/**
 * Just a holder for all necessary objects for server instance creation.
 *
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 28.09.15.
 */
public class Holder implements Closeable {

    public final FileManager fileManager;

    public final SessionDao sessionDao;

    public final UserDao userDao;

    public final TokenManager tokenManager;

    public final ReportingDao reportingDao;

    public final RedisClient redisClient;

    public final DBManager dbManager;

    public final GlobalStats stats;

    public final ServerProperties props;

    public final AverageAggregator averageAggregator;

    public final BlockingIOProcessor blockingIOProcessor;
    public final TransportTypeHolder transportTypeHolder;
    public final TwitterWrapper twitterWrapper;
    public final MailWrapper mailWrapper;
    public final GCMWrapper gcmWrapper;
    public final SMSWrapper smsWrapper;
    public final String region;
    public final TimerWorker timerWorker;

    public final EventorProcessor eventorProcessor;
    public final DefaultAsyncHttpClient asyncHttpClient;

    public final String currentIp;

    public Holder(ServerProperties serverProperties) {
        this.props = serverProperties;

        this.region = serverProperties.getProperty("region", "local");
        this.currentIp = serverProperties.getProperty("reset-pass.http.host", IPUtils.resolveHostIP());

        this.redisClient = new RedisClient(new ServerProperties(RedisClient.REDIS_PROPERTIES), region);

        String dataFolder = serverProperties.getProperty("data.folder");
        this.fileManager = new FileManager(dataFolder);
        this.sessionDao = new SessionDao();
        this.userDao = new UserDao(fileManager.deserialize(), this.region);
        this.blockingIOProcessor = new BlockingIOProcessor(
                serverProperties.getIntProperty("blocking.processor.thread.pool.limit", 5),
                serverProperties.getIntProperty("notifications.queue.limit", 10000),
                FileLoaderUtil.readFileAsString(BlockingIOProcessor.TOKEN_MAIL_BODY)
        );
        this.tokenManager = new TokenManager(this.userDao.users, blockingIOProcessor, redisClient, currentIp);
        this.stats = new GlobalStats();
        final String reportingFolder = getReportingFolder(dataFolder);
        this.averageAggregator = new AverageAggregator(reportingFolder);
        this.reportingDao = new ReportingDao(reportingFolder, averageAggregator, serverProperties);

        this.transportTypeHolder = new TransportTypeHolder(serverProperties);

        this.asyncHttpClient = new DefaultAsyncHttpClient(new DefaultAsyncHttpClientConfig.Builder()
                .setUserAgent(null)
                .setEventLoopGroup(transportTypeHolder.workerGroup)
                .setKeepAlive(false)
                .build()
        );

        this.twitterWrapper = new TwitterWrapper();
        this.mailWrapper = new MailWrapper(new ServerProperties(MailWrapper.MAIL_PROPERTIES_FILENAME), asyncHttpClient);
        this.gcmWrapper = new GCMWrapper(new ServerProperties(GCMWrapper.GCM_PROPERTIES_FILENAME), asyncHttpClient);
        this.smsWrapper = new SMSWrapper(new ServerProperties(SMSWrapper.SMS_PROPERTIES_FILENAME), asyncHttpClient);

        this.eventorProcessor = new EventorProcessor(gcmWrapper, twitterWrapper, blockingIOProcessor, stats);
        this.dbManager = new DBManager(blockingIOProcessor);
        this.timerWorker = new TimerWorker(userDao, sessionDao);
    }

    //for tests only
    public Holder(ServerProperties serverProperties, TwitterWrapper twitterWrapper, MailWrapper mailWrapper, GCMWrapper gcmWrapper, SMSWrapper smsWrapper) {
        this.props = serverProperties;

        this.region = "local";
        this.currentIp = serverProperties.getProperty("reset-pass.http.host", IPUtils.resolveHostIP());
        this.redisClient = new RedisClient(new ServerProperties(RedisClient.REDIS_PROPERTIES), "real");

        String dataFolder = serverProperties.getProperty("data.folder");
        this.fileManager = new FileManager(dataFolder);
        this.sessionDao = new SessionDao();
        this.userDao = new UserDao(fileManager.deserialize(), this.region);
        this.blockingIOProcessor = new BlockingIOProcessor(
                serverProperties.getIntProperty("blocking.processor.thread.pool.limit", 5),
                serverProperties.getIntProperty("notifications.queue.limit", 10000),
                FileLoaderUtil.readFileAsString(BlockingIOProcessor.TOKEN_MAIL_BODY)
        );
        this.tokenManager = new TokenManager(this.userDao.users, blockingIOProcessor, redisClient, currentIp);
        this.stats = new GlobalStats();
        final String reportingFolder = getReportingFolder(dataFolder);
        this.averageAggregator = new AverageAggregator(reportingFolder);
        this.reportingDao = new ReportingDao(reportingFolder, averageAggregator, serverProperties);

        this.transportTypeHolder = new TransportTypeHolder(serverProperties);

        this.twitterWrapper = twitterWrapper;
        this.mailWrapper = mailWrapper;
        this.gcmWrapper = gcmWrapper;
        this.smsWrapper = smsWrapper;

        this.eventorProcessor = new EventorProcessor(gcmWrapper, twitterWrapper, blockingIOProcessor, stats);
        this.asyncHttpClient = new DefaultAsyncHttpClient(new DefaultAsyncHttpClientConfig.Builder()
                .setUserAgent(null)
                .setEventLoopGroup(transportTypeHolder.workerGroup)
                .setKeepAlive(false)
                .build()
        );

        this.dbManager = new DBManager(blockingIOProcessor);
        this.timerWorker = new TimerWorker(userDao, sessionDao);
    }

    @Override
    public void close() {
        System.out.println("Stopping aggregator...");
        this.averageAggregator.close();

        System.out.println("Stopping BlockingIOProcessor...");
        this.blockingIOProcessor.close();

        System.out.println("Stopping DBManager...");
        this.dbManager.close();

        System.out.println("Stopping Transport Holder...");
        transportTypeHolder.close();

        redisClient.close();
    }
}
