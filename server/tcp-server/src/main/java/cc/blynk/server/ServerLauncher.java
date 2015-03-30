package cc.blynk.server;

import cc.blynk.common.stats.GlobalStats;
import cc.blynk.common.utils.Config;
import cc.blynk.common.utils.ServerProperties;
import cc.blynk.server.core.application.AppServer;
import cc.blynk.server.core.hardware.HardwareServer;
import cc.blynk.server.dao.FileManager;
import cc.blynk.server.dao.JedisWrapper;
import cc.blynk.server.dao.SessionsHolder;
import cc.blynk.server.dao.UserRegistry;
import cc.blynk.server.handlers.BaseSimpleChannelInboundHandler;
import cc.blynk.server.model.auth.User;
import cc.blynk.server.workers.ProfileSaverWorker;
import cc.blynk.server.workers.PropertiesChangeWatcherWorker;
import cc.blynk.server.workers.ShutdownHookWorker;
import cc.blynk.server.workers.timer.TimerWorker;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;

import java.util.List;
import java.util.Map;
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

    public static void main(String[] args) throws Exception {
        ServerProperties serverProperties = new ServerProperties();

        //required to make all loggers async with LMAX disruptor
        System.setProperty("Log4jContextSelector", "org.apache.logging.log4j.core.async.AsyncLoggerContextSelector");

        //configurable folder for logs via property.
        System.setProperty("logs.folder", serverProperties.getProperty("logs.folder"));

        //takes desired log level from properties
        changeLogLevel(Level.valueOf(serverProperties.getProperty("log.level")));

        new ArgumentsParser().processArguments(args, serverProperties);

        launch(serverProperties);
    }

    private static void changeLogLevel(Level newLevel) {
        LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
        Configuration conf = ctx.getConfiguration();
        conf.getLoggerConfig(LogManager.ROOT_LOGGER_NAME).setLevel(newLevel);
        ctx.updateLoggers(conf);
    }

    public static void launch(ServerProperties serverProperties) throws Exception {
        FileManager fileManager = new FileManager(serverProperties.getProperty("data.folder"));
        SessionsHolder sessionsHolder = new SessionsHolder();

        JedisWrapper jedisWrapper = new JedisWrapper(serverProperties);

        //first reading all data from disk
        Map<String, User> users = fileManager.deserialize();
        //after that getting full DB from Redis and adding here.
        users.putAll(jedisWrapper.getAllUsersDB());
        //todo save all to disk to have latest version locally???

        UserRegistry userRegistry = new UserRegistry(users);


        GlobalStats stats = new GlobalStats();

        HardwareServer hardwareServer = new HardwareServer(serverProperties, fileManager, userRegistry, sessionsHolder, stats);
        AppServer appServer = new AppServer(serverProperties, fileManager, userRegistry, sessionsHolder, stats);

        List<BaseSimpleChannelInboundHandler> baseHandlers = hardwareServer.getBaseHandlers();
        baseHandlers.addAll(appServer.getBaseHandlers());

        //start servers
        new Thread(appServer).start();
        new Thread(hardwareServer).start();


        //Launching all background jobs.
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);

        ProfileSaverWorker profileSaverWorker = new ProfileSaverWorker(jedisWrapper, userRegistry, fileManager, stats);
        scheduler.scheduleAtFixedRate(profileSaverWorker, 1000,
                serverProperties.getIntProperty("profile.save.worker.period"), TimeUnit.MILLISECONDS);

        //millis we need to wait to start scheduler at the beginning of a second.
        long startDelay = 1000 - (System.currentTimeMillis() % 1000);
        scheduler.scheduleAtFixedRate(
                new TimerWorker(userRegistry, sessionsHolder), startDelay, 1000, TimeUnit.MILLISECONDS);

        new Thread(new PropertiesChangeWatcherWorker(Config.SERVER_PROPERTIES_FILENAME, baseHandlers)).start();

        //todo test it works...
        Runtime.getRuntime().addShutdownHook(new Thread(new ShutdownHookWorker(hardwareServer, appServer, profileSaverWorker)));
    }

}
