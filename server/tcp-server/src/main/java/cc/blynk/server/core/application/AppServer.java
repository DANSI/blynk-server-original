package cc.blynk.server.core.application;

import cc.blynk.common.handlers.common.decoders.MessageDecoder;
import cc.blynk.common.handlers.common.encoders.MessageEncoder;
import cc.blynk.common.stats.GlobalStats;
import cc.blynk.common.utils.ServerProperties;
import cc.blynk.server.TransportTypeHolder;
import cc.blynk.server.core.BaseServer;
import cc.blynk.server.dao.SessionsHolder;
import cc.blynk.server.dao.UserRegistry;
import cc.blynk.server.handlers.app.AppChannelStateHandler;
import cc.blynk.server.handlers.app.auth.AppLoginHandler;
import cc.blynk.server.handlers.app.auth.AppShareLoginHandler;
import cc.blynk.server.handlers.app.auth.RegisterHandler;
import cc.blynk.server.handlers.common.UserNotLoggerHandler;
import cc.blynk.server.storage.StorageDao;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SslHandler;
import io.netty.handler.ssl.SslProvider;
import io.netty.handler.ssl.util.SelfSignedCertificate;
import io.netty.handler.timeout.ReadTimeoutHandler;

import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLException;
import java.io.File;
import java.security.cert.CertificateException;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 2/1/2015.
 */
public class AppServer extends BaseServer {

    private final ChannelInitializer<SocketChannel> channelInitializer;
    private boolean isMutualSSL;

    public AppServer(ServerProperties props, UserRegistry userRegistry, SessionsHolder sessionsHolder,
                     GlobalStats stats, TransportTypeHolder transportType, StorageDao storageDao) {
        super(props.getIntProperty("app.ssl.port"), transportType);

        RegisterHandler registerHandler = new RegisterHandler(userRegistry, props.getProperty("allowed.users.list"));
        AppLoginHandler appLoginHandler = new AppLoginHandler(props, userRegistry, sessionsHolder, storageDao);
        AppShareLoginHandler appShareLoginHandler = new AppShareLoginHandler(userRegistry, sessionsHolder);
        AppChannelStateHandler appChannelStateHandler = new AppChannelStateHandler(sessionsHolder);

        SslContext sslCtx = initSslContext(props);

        int appTimeoutSecs = props.getIntProperty("app.socket.idle.timeout", 0);
        log.debug("app.socket.idle.timeout = {}", appTimeoutSecs);

        this.channelInitializer = new ChannelInitializer<SocketChannel>() {
            @Override
            protected void initChannel(SocketChannel ch) throws Exception {
                ChannelPipeline pipeline = ch.pipeline();

                if (appTimeoutSecs > 0) {
                    pipeline.addLast(new ReadTimeoutHandler(appTimeoutSecs));
                }

                SSLEngine engine = sslCtx.newEngine(ch.alloc());
                if (isMutualSSL) {
                    engine.setUseClientMode(false);
                    engine.setNeedClientAuth(true);
                }
                pipeline.addLast(new SslHandler(engine));

                //non-sharable handlers
                pipeline.addLast(appChannelStateHandler);
                pipeline.addLast(new MessageDecoder(stats));
                pipeline.addLast(new MessageEncoder());

                //sharable business logic handlers initialized previously
                pipeline.addLast(registerHandler);
                pipeline.addLast(appLoginHandler);
                pipeline.addLast(new UserNotLoggerHandler());
            }
        };

        log.info("Application server port {}.", port);
    }

    private SslContext initSslContext(ServerProperties props) {
        log.info("Enabling SSL for application.");
        SslProvider sslProvider = props.getBoolProperty("enable.native.openssl") ? SslProvider.OPENSSL : SslProvider.JDK;
        if (sslProvider == SslProvider.OPENSSL) {
            log.warn("Using native openSSL provider for app SSL.");
        }
        return initSslContext(
                props.getProperty("server.ssl.cert"),
                props.getProperty("server.ssl.key"),
                props.getProperty("server.ssl.key.pass"),
                props.getProperty("client.ssl.cert"),
                sslProvider);
    }

    private SslContext initSslContext(String serverCertPath, String serverKeyPath, String serverPass,
                                             String clientCertPath,
                                             SslProvider sslProvider){
        try {
            File serverCert =  new File(serverCertPath);
            File serverKey = new File(serverKeyPath);
            File clientCert =  new File(clientCertPath);

            if (!serverCert.exists() || !serverKey.exists() || !clientCert.exists()) {
                log.warn("ATTENTION. Certificate {}, key {}, clietn cert {} paths not valid. Using embedded certs. This is not secure. Please replace it with your own certs.",
                        serverCert.getAbsolutePath(), serverKey.getAbsolutePath(), clientCert.getAbsolutePath());
                SelfSignedCertificate ssc = new SelfSignedCertificate();
                isMutualSSL = false;
                return SslContextBuilder.forServer(ssc.certificate(), ssc.privateKey())
                                        .sslProvider(sslProvider)
                                        .build();
            }

            isMutualSSL = true;
            return SslContextBuilder.forServer(serverCert, serverKey, serverPass)
                    .sslProvider(sslProvider)
                    .trustManager(clientCert)
                    .build();
        } catch (CertificateException | SSLException | IllegalArgumentException e) {
            log.error("Error initializing ssl context. Reason : {}", e.getMessage());
            throw new RuntimeException(e.getMessage());
        }
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
