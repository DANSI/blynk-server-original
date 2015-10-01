package cc.blynk.server;

import cc.blynk.common.utils.ServerProperties;
import cc.blynk.server.core.BaseServer;
import cc.blynk.server.core.administration.AdminServer;
import cc.blynk.server.core.application.AppServer;
import cc.blynk.server.core.hardware.HardwareServer;
import cc.blynk.server.core.hardware.ssl.HardwareSSLServer;
import cc.blynk.server.utils.LoggerUtil;

/**
 * Entry point for server launch.
 *
 * By default starts 3 server sockets on different ports:
 *
 * 1 server socket for SSL/TLS Hardware (8441 default)
 * 1 server socket for plain tcp/ip Hardware (8442 default)
 * 1 server socket for SSL/TLS Applications (8443 default)
 *
 * In addition launcher start all related to business logic threads like saving user profiles thread, timers
 * processing thread, properties reload thread and so on.
 *
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 2/16/2015.
 */
public class ServerLauncher {

    private final BaseServer appServer;
    private final BaseServer hardwareServer;
    private final BaseServer hardwareSSLServer;
    private final BaseServer adminServer;
    private final Holder holder;

    private ServerLauncher(ServerProperties serverProperties) {
        this.holder = new Holder(serverProperties);

        this.hardwareServer = new HardwareServer(holder);
        this.hardwareSSLServer = new HardwareSSLServer(holder);
        this.appServer = new AppServer(holder);
        this.adminServer = new AdminServer(holder);

    }

    public static void main(String[] args) throws Exception {
        ServerProperties serverProperties = new ServerProperties();

        //required to make all loggers async with LMAX disruptor
        System.setProperty("Log4jContextSelector", "org.apache.logging.log4j.core.async.AsyncLoggerContextSelector");
        System.setProperty("AsyncLogger.RingBufferSize",
                serverProperties.getProperty("async.logger.ring.buffer.size", String.valueOf(8 * 1024)));

        //configurable folder for logs via property.
        System.setProperty("logs.folder", serverProperties.getProperty("logs.folder"));

        //changing log level based on properties file
        LoggerUtil.changeLogLevel(serverProperties.getProperty("log.level"));

        ArgumentsParser.parse(args, serverProperties);

        System.setProperty("data.folder", serverProperties.getProperty("data.folder"));

        new ServerLauncher(serverProperties).run();
    }

    private void run() {
        //start servers
        new Thread(appServer).start();
        new Thread(hardwareServer).start();
        new Thread(hardwareSSLServer).start();
        new Thread(adminServer).start();

        //Launching all background jobs.
        JobLauncher.start(holder, hardwareServer, appServer, adminServer, hardwareSSLServer);
    }

}
