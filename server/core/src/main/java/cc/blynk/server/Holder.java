package cc.blynk.server;

import cc.blynk.server.core.BlockingIOProcessor;
import cc.blynk.server.core.dao.FileManager;
import cc.blynk.server.core.dao.ReportingStorageDao;
import cc.blynk.server.core.dao.SessionDao;
import cc.blynk.server.core.dao.TokenManager;
import cc.blynk.server.core.dao.UserDao;
import cc.blynk.server.core.dao.ota.OTAManager;
import cc.blynk.server.core.model.widgets.ui.reporting.ReportScheduler;
import cc.blynk.server.core.processors.EventorProcessor;
import cc.blynk.server.core.stats.GlobalStats;
import cc.blynk.server.db.DBManager;
import cc.blynk.server.internal.TokensPool;
import cc.blynk.server.notifications.mail.MailWrapper;
import cc.blynk.server.notifications.push.GCMWrapper;
import cc.blynk.server.notifications.sms.SMSWrapper;
import cc.blynk.server.notifications.twitter.TwitterWrapper;
import cc.blynk.server.transport.TransportTypeHolder;
import cc.blynk.server.workers.ReadingWidgetsWorker;
import cc.blynk.server.workers.timer.TimerWorker;
import cc.blynk.utils.FileUtils;
import cc.blynk.utils.properties.GCMProperties;
import cc.blynk.utils.properties.MailProperties;
import cc.blynk.utils.properties.ServerProperties;
import cc.blynk.utils.properties.SmsProperties;
import cc.blynk.utils.properties.TwitterProperties;
import io.netty.channel.epoll.Epoll;
import io.netty.util.internal.SystemPropertyUtil;
import org.asynchttpclient.DefaultAsyncHttpClient;
import org.asynchttpclient.DefaultAsyncHttpClientConfig;

import java.util.Collections;

import static cc.blynk.server.internal.ReportingUtil.getReportingFolder;

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

    public final TokenManager tokenManager;

    public final ReportingStorageDao reportingDao;

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
    public final ReportScheduler reportScheduler;

    public final EventorProcessor eventorProcessor;
    public final DefaultAsyncHttpClient asyncHttpClient;

    public final OTAManager otaManager;

    public final Limits limits;
    public final TextHolder textHolder;

    public final String downloadUrl;

    public final String host;

    public final SslContextHolder sslContextHolder;

    public final TokensPool tokensPool;

    public Holder(ServerProperties serverProperties, MailProperties mailProperties,
                  SmsProperties smsProperties, GCMProperties gcmProperties, TwitterProperties twitterProperties,
                  boolean restore) {
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
        this.reportingDao = new ReportingStorageDao(reportingFolder,
                serverProperties.isRawDBEnabled() && dbManager.isDBEnabled());

        if (serverProperties.renameOldReportingFiles()) {
            reportingDao.renameOldReportingFiles();
        }

        this.transportTypeHolder = new TransportTypeHolder(serverProperties);

        this.asyncHttpClient = new DefaultAsyncHttpClient(new DefaultAsyncHttpClientConfig.Builder()
                .setUserAgent(null)
                .setKeepAlive(true)
                .setUseNativeTransport(Epoll.isAvailable())
                .setUseOpenSsl(SslContextHolder.isOpenSslAvailable())
                .build()
        );

        String productName = serverProperties.getProductName();
        this.twitterWrapper = new TwitterWrapper(twitterProperties, asyncHttpClient);
        this.mailWrapper = new MailWrapper(mailProperties, productName);
        this.gcmWrapper = new GCMWrapper(gcmProperties, asyncHttpClient, productName);
        this.smsWrapper = new SMSWrapper(smsProperties, asyncHttpClient);

        this.otaManager = new OTAManager(props);

        this.eventorProcessor = new EventorProcessor(
                gcmWrapper, mailWrapper, twitterWrapper, blockingIOProcessor, stats);
        this.timerWorker = new TimerWorker(userDao, sessionDao, gcmWrapper);
        this.readingWidgetsWorker = new ReadingWidgetsWorker(sessionDao, userDao, props.getAllowWithoutActiveApp());
        this.limits = new Limits(props);
        this.textHolder = new TextHolder(gcmProperties);

        this.downloadUrl = FileUtils.downloadUrl(host,
                props.getProperty("http.port"),
                props.getBoolProperty("force.port.80.for.csv")
        );
        this.reportScheduler = new ReportScheduler(1, downloadUrl, mailWrapper, reportingDao, userDao.users);

        String contactEmail = serverProperties.getProperty("contact.email", mailProperties.getSMTPUsername());
        this.sslContextHolder = new SslContextHolder(props, contactEmail);
        this.tokensPool = new TokensPool(60 * 60 * 1000);
    }

    //for tests only
    public Holder(ServerProperties serverProperties, TwitterWrapper twitterWrapper,
                  MailWrapper mailWrapper,
                  GCMWrapper gcmWrapper, SMSWrapper smsWrapper,
                  String dbFileName) {
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
        this.reportingDao = new ReportingStorageDao(reportingFolder,
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
        this.readingWidgetsWorker = new ReadingWidgetsWorker(sessionDao, userDao, props.getAllowWithoutActiveApp());
        this.limits = new Limits(props);
        this.textHolder = new TextHolder(new GCMProperties(Collections.emptyMap()));

        this.downloadUrl = FileUtils.downloadUrl(host,
                props.getProperty("http.port"),
                props.getBoolProperty("force.port.80.for.csv")
        );
        this.reportScheduler = new ReportScheduler(1, downloadUrl, mailWrapper, reportingDao, userDao.users);

        this.sslContextHolder = new SslContextHolder(props, "test@blynk.cc");
        this.tokensPool = new TokensPool(60 * 60 * 1000);
    }

    private static void disableNettyLeakDetector() {
        String leakProperty = SystemPropertyUtil.get("io.netty.leakDetection.level");
        //we do not pass any with JVM option
        if (leakProperty == null) {
            System.setProperty("io.netty.leakDetection.level", "disabled");
        }
    }

    public boolean isLocalRegion() {
        return region.equals("local");
    }

    public void close() {
        sessionDao.close();

        transportTypeHolder.close();

        reportingDao.close();

        System.out.println("Stopping BlockingIOProcessor...");
        blockingIOProcessor.close();
        reportScheduler.shutdown();
        System.out.println("Stopping DBManager...");
        dbManager.close();
    }
}
