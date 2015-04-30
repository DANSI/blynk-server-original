package cc.blynk.server.core.hardware.ssl;

import cc.blynk.common.stats.GlobalStats;
import cc.blynk.common.utils.ServerProperties;
import cc.blynk.server.TransportTypeHolder;
import cc.blynk.server.core.BaseServer;
import cc.blynk.server.core.hardware.HardwareChannelInitializer;
import cc.blynk.server.core.hardware.HardwareHandlersHolder;
import cc.blynk.server.dao.SessionsHolder;
import cc.blynk.server.dao.UserRegistry;
import cc.blynk.server.handlers.BaseSimpleChannelInboundHandler;
import cc.blynk.server.workers.notifications.NotificationsProcessor;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslProvider;
import io.netty.handler.ssl.util.SelfSignedCertificate;

import javax.net.ssl.SSLException;
import java.io.File;
import java.security.cert.CertificateException;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 2/1/2015.
 */
public class HardwareSSLServer extends BaseServer {

    private final HardwareHandlersHolder handlersHolder;
    private final ChannelInitializer<SocketChannel> channelInitializer;

    public HardwareSSLServer(ServerProperties props, UserRegistry userRegistry, SessionsHolder sessionsHolder,
                             GlobalStats stats, NotificationsProcessor notificationsProcessor, TransportTypeHolder transportType) {
        super(props.getIntProperty("hardware.ssl.port"), transportType);

        this.handlersHolder = new HardwareHandlersHolder(props, userRegistry, sessionsHolder, notificationsProcessor);
        int hardTimeoutSecs = props.getIntProperty("hard.socket.idle.timeout", 15);

        SslContext sslContext = initSslContext(
                props.getProperty("server.ssl.cert"),
                props.getProperty("server.ssl.key"),
                props.getProperty("server.ssl.key.pass"),
                props.getBoolProperty("enable.native.openssl") ? SslProvider.OPENSSL : SslProvider.JDK);

        this.channelInitializer = new HardwareChannelInitializer(sessionsHolder, stats, handlersHolder, hardTimeoutSecs, sslContext);

        log.info("SSL hardware port {}.", port);
    }

    private SslContext initSslContext(String serverCertPath, String serverKeyPath, String serverPass,
                                      SslProvider sslProvider) {
        try {
            if (sslProvider == SslProvider.OPENSSL) {
                log.warn("Using native openSSL provider.");
            }

            File serverCert =  new File(serverCertPath);
            File serverKey = new File(serverKeyPath);

            if (!serverCert.exists() || !serverKey.exists()) {
                log.warn("ATTENTION. Certificate path not valid. Using embedded certs. This is not secure. Please replace it with your own certs.");
                SelfSignedCertificate ssc = new SelfSignedCertificate();
                return SslContext.newServerContext(sslProvider, ssc.certificate(), ssc.privateKey());
            }

            return SslContext.newServerContext(sslProvider, serverCert, serverKey, serverPass);
        } catch (CertificateException | SSLException | IllegalArgumentException e) {
            log.error("Error initializing ssl context. Reason : {}", e.getMessage());
            throw new RuntimeException(e.getMessage());
        }
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
        log.info("Shutting down default server...");
        super.stop();
    }

}
