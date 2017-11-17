package cc.blynk.server;

import cc.blynk.server.core.BlockingIOProcessor;
import cc.blynk.server.core.dao.FileManager;
import cc.blynk.server.core.dao.ReportingDao;
import cc.blynk.server.core.dao.SessionDao;
import cc.blynk.server.core.dao.TokenManager;
import cc.blynk.server.core.dao.UserDao;
import cc.blynk.server.core.dao.ota.OTAManager;
import cc.blynk.server.core.processors.EventorProcessor;
import cc.blynk.server.core.stats.GlobalStats;
import cc.blynk.server.db.DBManager;
import cc.blynk.server.notifications.mail.MailWrapper;
import cc.blynk.server.notifications.push.GCMWrapper;
import cc.blynk.server.notifications.sms.SMSWrapper;
import cc.blynk.server.notifications.twitter.TwitterWrapper;
import cc.blynk.server.transport.TransportTypeHolder;
import cc.blynk.server.workers.ReadingWidgetsWorker;
import cc.blynk.server.workers.timer.TimerWorker;
import cc.blynk.utils.FileUtils;
import cc.blynk.utils.properties.BaseProperties;
import cc.blynk.utils.properties.ServerProperties;
import io.netty.channel.epoll.Epoll;
import io.netty.util.internal.SystemPropertyUtil;
import org.asynchttpclient.DefaultAsyncHttpClient;
import org.asynchttpclient.DefaultAsyncHttpClientConfig;

import java.io.Closeable;

import static cc.blynk.server.internal.ReportingUtil.getReportingFolder;

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

    public final DBManager dbManager;

    public final GlobalStats stats;

    public final ServerProperties props;

    public final BlockingIOProcessor blockingIOProcessor;
    public final TransportTypeHolder transportTypeHolder;
    public final TwitterWrapper twitterWrapper;
    public final MailWrapper mailWrapper;
    public final GCMWrapper gcmWrapper;
    public final SMSWrapper smsWrapper;
    public final String region;
    public final TimerWorker timerWorker;
    public final ReadingWidgetsWorker readingWidgetsWorker;

    public final EventorProcessor eventorProcessor;
    public final DefaultAsyncHttpClient asyncHttpClient;

    public final OTAManager otaManager;

    public final Limits limits;

    public final String csvDownloadUrl;

    public final String host;

    public final SslContextHolder sslContextHolder;

    public Holder(ServerProperties serverProperties, BaseProperties mailProperties,
                  BaseProperties smsProperties, BaseProperties gcmProperties, boolean restore) {
        disableNettyLeakDetector();
        this.props = serverProperties;

        this.region = serverProperties.getProperty("region", "local");
        this.host = serverProperties.getServerHost();

        String dataFolder = serverProperties.getProperty("data.folder");
        this.fileManager = new FileManager(dataFolder, host);
        this.sessionDao = new SessionDao();
        this.blockingIOProcessor = new BlockingIOProcessor(
                serverProperties.getIntProperty("blocking.processor.thread.pool.limit", 6),
                serverProperties.getIntProperty("notifications.queue.limit", 2000)
        );
        this.dbManager = new DBManager(blockingIOProcessor, serverProperties.getBoolProperty("enable.db"));

        if (restore) {
            try {
                this.userDao = new UserDao(dbManager.userDBDao.getAllUsers(this.region), this.region, host);
            } catch (Exception e) {
                System.out.println("Error restoring data from DB!");
                e.printStackTrace();
                throw new RuntimeException(e);
            }
        } else {
            this.userDao = new UserDao(fileManager.deserializeUsers(), this.region, host);
        }

        this.tokenManager = new TokenManager(this.userDao.users, dbManager, host);
        this.stats = new GlobalStats();
        final String reportingFolder = getReportingFolder(dataFolder);
        this.reportingDao = new ReportingDao(reportingFolder,
                serverProperties.isRawDBEnabled() && dbManager.isDBEnabled());

        this.transportTypeHolder = new TransportTypeHolder(serverProperties);

        this.asyncHttpClient = new DefaultAsyncHttpClient(new DefaultAsyncHttpClientConfig.Builder()
                .setUserAgent(null)
                .setKeepAlive(true)
                .setUseNativeTransport(Epoll.isAvailable())
                .setUseOpenSsl(SslContextHolder.isOpenSslAvailable())
                .build()
        );

        this.twitterWrapper = new TwitterWrapper();
        this.mailWrapper = new MailWrapper(mailProperties);
        this.gcmWrapper = new GCMWrapper(gcmProperties, asyncHttpClient);
        this.smsWrapper = new SMSWrapper(smsProperties, asyncHttpClient);

        this.otaManager = new OTAManager(props);

        this.eventorProcessor = new EventorProcessor(
                gcmWrapper, mailWrapper, twitterWrapper, blockingIOProcessor, stats);
        this.timerWorker = new TimerWorker(userDao, sessionDao, gcmWrapper);
        this.readingWidgetsWorker = new ReadingWidgetsWorker(sessionDao, userDao);
        this.limits = new Limits(props);

        this.csvDownloadUrl = FileUtils.csvDownloadUrl(host, props.getProperty("http.port"));

        String contactEmail = serverProperties.getProperty("contact.email",
                mailProperties.getProperty("mail.smtp.username"));
        this.sslContextHolder = new SslContextHolder(props, contactEmail);
    }

    //for tests only
    public Holder(ServerProperties serverProperties, TwitterWrapper twitterWrapper,
                  MailWrapper mailWrapper, GCMWrapper gcmWrapper, SMSWrapper smsWrapper, String dbFileName) {
        disableNettyLeakDetector();
        this.props = serverProperties;

        this.region = "local";
        this.host = serverProperties.getServerHost();

        String dataFolder = serverProperties.getProperty("data.folder");
        this.fileManager = new FileManager(dataFolder, host);
        this.sessionDao = new SessionDao();
        this.userDao = new UserDao(fileManager.deserializeUsers(), this.region, host);
        this.blockingIOProcessor = new BlockingIOProcessor(
                serverProperties.getIntProperty("blocking.processor.thread.pool.limit", 5),
                serverProperties.getIntProperty("notifications.queue.limit", 2000)
        );

        this.dbManager = new DBManager(dbFileName, blockingIOProcessor, serverProperties.getBoolProperty("enable.db"));
        this.tokenManager = new TokenManager(this.userDao.users, dbManager, host);
        this.stats = new GlobalStats();
        final String reportingFolder = getReportingFolder(dataFolder);
        this.reportingDao = new ReportingDao(reportingFolder,
                serverProperties.isRawDBEnabled() && dbManager.isDBEnabled());

        this.transportTypeHolder = new TransportTypeHolder(serverProperties);

        this.twitterWrapper = twitterWrapper;
        this.mailWrapper = mailWrapper;
        this.gcmWrapper = gcmWrapper;
        this.smsWrapper = smsWrapper;

        this.otaManager = new OTAManager(props);

        this.eventorProcessor = new EventorProcessor(
                gcmWrapper, mailWrapper, twitterWrapper, blockingIOProcessor, stats);
        this.asyncHttpClient = new DefaultAsyncHttpClient(new DefaultAsyncHttpClientConfig.Builder()
                .setUserAgent(null)
                .setKeepAlive(true)
                .setUseNativeTransport(Epoll.isAvailable())
                .setUseOpenSsl(SslContextHolder.isOpenSslAvailable())
                .build()
        );

        this.timerWorker = new TimerWorker(userDao, sessionDao, gcmWrapper);
        this.readingWidgetsWorker = new ReadingWidgetsWorker(sessionDao, userDao);
        this.limits = new Limits(props);

        this.csvDownloadUrl = FileUtils.csvDownloadUrl(host, props.getProperty("http.port"));

        this.sslContextHolder = new SslContextHolder(props, "test@blynk.cc");
    }

    private static void disableNettyLeakDetector() {
        String leakProperty = SystemPropertyUtil.get("io.netty.leakDetection.level");
        //we do not pass any with JVM option
        if (leakProperty == null) {
            System.setProperty("io.netty.leakDetection.level", "disabled");
        }
    }

    @Override
    public void close() {
        this.reportingDao.close();

        System.out.println("Stopping BlockingIOProcessor...");
        this.blockingIOProcessor.close();

        System.out.println("Stopping DBManager...");
        this.dbManager.close();

        System.out.println("Stopping Transport Holder...");
        transportTypeHolder.close();
    }
}
