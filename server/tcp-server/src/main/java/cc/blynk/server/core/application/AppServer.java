package cc.blynk.server.core.application;

import cc.blynk.common.stats.GlobalStats;
import cc.blynk.common.utils.ServerProperties;
import cc.blynk.server.TransportTypeHolder;
import cc.blynk.server.core.BaseServer;
import cc.blynk.server.dao.SessionsHolder;
import cc.blynk.server.dao.UserRegistry;
import cc.blynk.server.handlers.BaseSimpleChannelInboundHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.ssl.IdentityCipherSuiteFilter;
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
public class AppServer extends BaseServer {

    private final AppHandlersHolder handlersHolder;
    private final ChannelInitializer<SocketChannel> channelInitializer;
    private boolean isMutualSSL;

    public AppServer(ServerProperties props, UserRegistry userRegistry, SessionsHolder sessionsHolder,
                     GlobalStats stats, TransportTypeHolder transportType) {
        super(props.getIntProperty("app.ssl.port"), transportType);

        this.handlersHolder = new AppHandlersHolder(props, userRegistry, sessionsHolder);

        log.info("Enabling SSL for application.");
        SslProvider sslProvider = props.getBoolProperty("enable.native.openssl") ? SslProvider.OPENSSL : SslProvider.JDK;
        if (sslProvider == SslProvider.OPENSSL) {
            log.warn("Using native openSSL provider for app SSL.");
        }
        SslContext sslContext = initSslContext(
                props.getProperty("server.ssl.cert"),
                props.getProperty("server.ssl.key"),
                props.getProperty("server.ssl.key.pass"),
                props.getProperty("client.ssl.cert"),
                sslProvider);

        int appTimeoutSecs = props.getIntProperty("app.socket.idle.timeout", 600);
        log.debug("app.socket.idle.timeout = {}", appTimeoutSecs);
        this.channelInitializer = new AppChannelInitializer(sessionsHolder, stats, handlersHolder, sslContext, appTimeoutSecs, isMutualSSL);

        log.info("Application server port {}.", port);
    }

    private SslContext initSslContext(String serverCertPath, String serverKeyPath, String serverPass,
                                             String clientCertPath,
                                             SslProvider sslProvider) {
        try {
            File serverCert =  new File(serverCertPath);
            File serverKey = new File(serverKeyPath);
            File clientCert =  new File(clientCertPath);

            if (!serverCert.exists() || !serverKey.exists() || !clientCert.exists()) {
                log.warn("ATTENTION. Certificate path not valid. Using embedded certs. This is not secure. Please replace it with your own certs.");
                SelfSignedCertificate ssc = new SelfSignedCertificate();
                isMutualSSL = false;
                return SslContext.newServerContext(sslProvider, ssc.certificate(), ssc.privateKey());
            }

            isMutualSSL = true;
            return SslContext.newServerContext(sslProvider, clientCert, null, serverCert, serverKey, serverPass,
                    null, null, IdentityCipherSuiteFilter.INSTANCE, null, 0 ,0);
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
        log.info("Shutting down SSL server...");
        super.stop();
    }

}
