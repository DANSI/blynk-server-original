package cc.blynk.server.core.hardware.ssl;

import cc.blynk.common.handlers.common.decoders.MessageDecoder;
import cc.blynk.common.handlers.common.encoders.MessageEncoder;
import cc.blynk.common.stats.GlobalStats;
import cc.blynk.common.utils.ServerProperties;
import cc.blynk.server.TransportTypeHolder;
import cc.blynk.server.core.BaseServer;
import cc.blynk.server.dao.SessionsHolder;
import cc.blynk.server.dao.UserRegistry;
import cc.blynk.server.handlers.hardware.HardwareChannelStateHandler;
import cc.blynk.server.handlers.hardware.HardwareHandler;
import cc.blynk.server.handlers.hardware.auth.HardwareLoginHandler;
import cc.blynk.server.storage.StorageDao;
import cc.blynk.server.workers.notifications.NotificationsProcessor;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SslProvider;
import io.netty.handler.ssl.util.SelfSignedCertificate;
import io.netty.handler.timeout.ReadTimeoutHandler;

import javax.net.ssl.SSLException;
import java.io.File;
import java.security.cert.CertificateException;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 2/1/2015.
 */
public class HardwareSSLServer extends BaseServer {

    private final ChannelInitializer<SocketChannel> channelInitializer;

    public HardwareSSLServer(ServerProperties props, UserRegistry userRegistry, SessionsHolder sessionsHolder,
                             GlobalStats stats, NotificationsProcessor notificationsProcessor, TransportTypeHolder transportType, StorageDao storageDao) {
        super(props.getIntProperty("hardware.ssl.port"), transportType);

        HardwareLoginHandler hardwareLoginHandler = new HardwareLoginHandler(userRegistry, sessionsHolder);
        HardwareChannelStateHandler hardwareChannelStateHandler = new HardwareChannelStateHandler(sessionsHolder, notificationsProcessor);

        int hardTimeoutSecs = props.getIntProperty("hard.socket.idle.timeout", 0);

        SslContext sslCtx = initSslContext(props);

        this.channelInitializer = new ChannelInitializer<SocketChannel>() {
            @Override
            protected void initChannel(SocketChannel ch) throws Exception {
                ChannelPipeline pipeline = ch.pipeline();
                //non-sharable handlers
                if (hardTimeoutSecs > 0) {
                    pipeline.addLast(new ReadTimeoutHandler(hardTimeoutSecs));
                }
                pipeline.addLast(sslCtx.newHandler(ch.alloc()));

                pipeline.addLast(hardwareChannelStateHandler);
                pipeline.addLast(new MessageDecoder(stats));
                pipeline.addLast(new MessageEncoder());

                //sharable business logic handlers
                pipeline.addLast(hardwareLoginHandler);
                pipeline.addLast(new HardwareHandler(props, sessionsHolder, storageDao, notificationsProcessor));
            }
        };

        log.info("SSL hardware port {}.", port);
    }

    private SslContext initSslContext(ServerProperties props) {
        SslProvider sslProvider = props.getBoolProperty("enable.native.openssl") ? SslProvider.OPENSSL : SslProvider.JDK;
        if (sslProvider == SslProvider.OPENSSL) {
            log.warn("Using native openSSL provider for hardware SSL.");
        }

        return initSslContext(props.getProperty("server.ssl.cert"),
                props.getProperty("server.ssl.key"),
                props.getProperty("server.ssl.key.pass"),
                sslProvider);
    }

    private SslContext initSslContext(String serverCertPath, String serverKeyPath, String serverPass,
                                      SslProvider sslProvider) {
        try {
            File serverCert =  new File(serverCertPath);
            File serverKey = new File(serverKeyPath);

            if (!serverCert.exists() || !serverKey.exists()) {
                log.warn("ATTENTION. Certificate {} and key {} paths not valid. Using embedded certs. This is not secure. Please replace it with your own certs.",
                        serverCert.getAbsolutePath(), serverKey.getAbsolutePath());
                SelfSignedCertificate ssc = new SelfSignedCertificate();
                return SslContextBuilder.forServer(ssc.certificate(), ssc.privateKey()).sslProvider(sslProvider).build();
            }

            return SslContextBuilder.forServer(serverCert, serverKey, serverPass)
                    .sslProvider(sslProvider)
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
        log.info("Shutting down default server...");
        super.stop();
    }

}
