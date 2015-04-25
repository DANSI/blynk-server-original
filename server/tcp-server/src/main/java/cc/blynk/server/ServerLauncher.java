package cc.blynk.server;

import cc.blynk.common.stats.GlobalStats;
import cc.blynk.common.utils.ServerProperties;
import cc.blynk.server.core.BaseServer;
import cc.blynk.server.core.administration.AdminServer;
import cc.blynk.server.core.application.AppServer;
import cc.blynk.server.core.hardware.HardwareServer;
import cc.blynk.server.dao.FileManager;
import cc.blynk.server.dao.JedisWrapper;
import cc.blynk.server.dao.SessionsHolder;
import cc.blynk.server.dao.UserRegistry;
import cc.blynk.server.handlers.BaseSimpleChannelInboundHandler;
import cc.blynk.server.workers.ProfileSaverWorker;
import cc.blynk.server.workers.PropertiesChangeWatcherWorker;
import cc.blynk.server.workers.ShutdownHookWorker;
import cc.blynk.server.workers.StatsWorker;
import cc.blynk.server.workers.notifications.NotificationsProcessor;
import cc.blynk.server.workers.timer.TimerWorker;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Entry point for server launch.
 *
 * By default starts 2 servers on different ports.
 * First is plain tcp/ip sockets server for hardware, second tls/ssl tcp/ip server for mobile applications.
 *
 * In addition launcher start all related to business logic threads like saving user profiles thread, timers
 * processing thread, properties reload thread and so on.
 *
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 2/16/2015.
 */
public class ServerLauncher {

    private final FileManager fileManager;
    private final SessionsHolder sessionsHolder;
    private final JedisWrapper jedisWrapper;
    private final UserRegistry userRegistry;
    private final GlobalStats stats;
    private final BaseServer appServer;
    private final BaseServer hardwareServer;
    private final BaseServer adminServer;
    private final ServerProperties serverProperties;
    private final NotificationsProcessor notificationsProcessor;

    private ServerLauncher(ServerProperties serverProperties) {
        this.serverProperties = serverProperties;
        this.fileManager = new FileManager(serverProperties.getProperty("data.folder"));
        this.sessionsHolder = new SessionsHolder();
        this.jedisWrapper = new JedisWrapper(serverProperties);
        //todo save all to disk to have latest version locally???
        this.userRegistry = new UserRegistry(fileManager.deserialize(), jedisWrapper.getAllUsersDB());
        this.stats = new GlobalStats();

        this.notificationsProcessor = new NotificationsProcessor(
                serverProperties.getIntProperty("notifications.queue.limit", 10000)
        );

        TransportTypeHolder transportType = new TransportTypeHolder(serverProperties);

        this.hardwareServer = new HardwareServer(serverProperties, userRegistry, sessionsHolder, stats, notificationsProcessor, transportType);
        this.appServer = new AppServer(serverProperties, userRegistry, sessionsHolder, stats, transportType);
        this.adminServer = new AdminServer(serverProperties, userRegistry, sessionsHolder, transportType);

    }

    public static void main(String[] args) throws Exception {
        ServerProperties serverProperties = new ServerProperties();

        //required to make all loggers async with LMAX disruptor
        System.setProperty("Log4jContextSelector", "org.apache.logging.log4j.core.async.AsyncLoggerContextSelector");

        //configurable folder for logs via property.
        System.setProperty("logs.folder", serverProperties.getProperty("logs.folder"));

        changeLogLevel(serverProperties.getProperty("log.level"));

        new ArgumentsParser().processArguments(args, serverProperties);

        new ServerLauncher(serverProperties).start();
    }

    /**
     * Sets desired log level from properties.
     *
     * @param level - desired log level. error|info|debug|trace, etc.
     */
    private static void changeLogLevel(String level) {
        Level newLevel = Level.valueOf(level);
        LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
        Configuration conf = ctx.getConfiguration();
        conf.getLoggerConfig(LogManager.ROOT_LOGGER_NAME).setLevel(newLevel);
        ctx.updateLoggers(conf);
    }

    private void start() {
        //start servers
        new Thread(appServer).start();
        new Thread(hardwareServer).start();
        new Thread(adminServer).start();

        //Launching all background jobs.
        startJobs();
    }

    private void startJobs() {
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(3);

        ProfileSaverWorker profileSaverWorker = new ProfileSaverWorker(jedisWrapper, userRegistry, fileManager);
        scheduler.scheduleAtFixedRate(profileSaverWorker, 1000,
                serverProperties.getIntProperty("profile.save.worker.period"), TimeUnit.MILLISECONDS);

        StatsWorker statsWorker = new StatsWorker(stats, sessionsHolder);
        scheduler.scheduleAtFixedRate(statsWorker, 1000,
                serverProperties.getIntProperty("stats.print.worker.period"), TimeUnit.MILLISECONDS);

        //millis we need to wait to start scheduler at the beginning of a second.
        long startDelay = 1000 - (System.currentTimeMillis() % 1000);
        scheduler.scheduleAtFixedRate(
                new TimerWorker(userRegistry, sessionsHolder), startDelay, 1000, TimeUnit.MILLISECONDS);

        List<BaseSimpleChannelInboundHandler> baseHandlers = new ArrayList<>(Arrays.asList(hardwareServer.getBaseHandlers()));
        baseHandlers.addAll(Arrays.asList(appServer.getBaseHandlers()));
        new Thread(new PropertiesChangeWatcherWorker(baseHandlers)).start();

        //todo test it works...
        Runtime.getRuntime().addShutdownHook(new Thread(new ShutdownHookWorker(profileSaverWorker, notificationsProcessor, hardwareServer, appServer, adminServer)));
    }

}
