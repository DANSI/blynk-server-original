package cc.blynk.server.core.application;

import cc.blynk.common.stats.GlobalStats;
import cc.blynk.common.utils.ServerProperties;
import cc.blynk.server.core.BaseServer;
import cc.blynk.server.dao.FileManager;
import cc.blynk.server.dao.SessionsHolder;
import cc.blynk.server.dao.UserRegistry;
import cc.blynk.server.handlers.BaseSimpleChannelInboundHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.ssl.SslContext;

import javax.net.ssl.SSLException;
import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 2/1/2015.
 */
public class AppServer extends BaseServer {

    private final AppHandlersHolder handlersHolder;
    private final ChannelInitializer<SocketChannel> channelInitializer;

    public AppServer(ServerProperties props, FileManager fileManager, UserRegistry userRegistry, SessionsHolder sessionsHolder, GlobalStats stats) {
        super(props.getIntProperty("server.ssl.port"),
              props.getIntProperty("server.worker.threads", Runtime.getRuntime().availableProcessors()),
              props.getBoolProperty("enable.native.epoll.transport"));

        this.handlersHolder = new AppHandlersHolder(props, fileManager, userRegistry, sessionsHolder);

        boolean sslEnabled = props.getBoolProperty("app.ssl.enabled");
        SslContext sslContext = null;
        if (sslEnabled) {
            log.info("SSL for Application enabled.");
            sslContext = initSslContext(props.getProperty("server.ssl.cert"),
                    props.getProperty("server.ssl.key"),
                    props.getProperty("server.ssl.key.pass"));
        } else {
            log.warn("SSL is disabled!");
        }

        int appTimeoutSecs = props.getIntProperty("app.socket.idle.timeout", 600);
        log.debug("app.socket.idle.timeout = {}", appTimeoutSecs);
        this.channelInitializer = new AppChannelInitializer(sessionsHolder, stats, handlersHolder, sslContext, appTimeoutSecs);

        log.info("Application server port {}.", port);
    }

    public static SslContext initSslContext(String serverCertPath, String serverKeyPath, String keyPass) {
        try {
            File serverCert = resolvePath(serverCertPath);
            File serverKey = resolvePath(serverKeyPath);

            //todo this is self-signed cerf. just to simplify for now for testing.
            return SslContext.newServerContext(serverCert, serverKey, keyPass);
        } catch (URISyntaxException e) {
            log.error("Error initializing certs path. Reason : {} ", e.getMessage());
            System.exit(0);
        } catch (SSLException e) {
            log.error("Error initializing ssl context. Reason : {}", e.getMessage());
            System.exit(0);
           //todo throw?
        } catch (IllegalStateException e) {
            log.error(e.getMessage());
            System.exit(0);
        }
        return null;
    }

    private static File resolvePath(String pathToFile) throws URISyntaxException {
        File file = new File(pathToFile);
        if (!file.exists()) {
            URL url = AppServer.class.getResource(pathToFile);
            if (url == null) {
                throw new IllegalStateException(pathToFile + " - not available.");
            }
            file = new File(url.toURI());
        }

        return file;
    }

    @Override
    public BaseSimpleChannelInboundHandler[] getBaseHandlers() {
        return handlersHolder.getBaseHandlers();
    }

    @Override
    public ChannelInitializer<SocketChannel> getChannelInitializer() {
        return channelInitializer;
    }

    @Override
    public void stop() {
        log.info("Shutting down SSL server...");
        super.stop();
    }

}
