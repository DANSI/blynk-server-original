package cc.blynk.server.utils;

import cc.blynk.common.utils.ServerProperties;
import cc.blynk.server.core.application.AppSslContext;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SslProvider;
import io.netty.handler.ssl.util.SelfSignedCertificate;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.net.ssl.SSLException;
import java.io.File;
import java.security.cert.CertificateException;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 28.09.15.
 */
public class SslUtil {

    private final static Logger log = LogManager.getLogger(SslUtil.class);

    public static AppSslContext initSslContext(ServerProperties props) {
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

    private static AppSslContext initSslContext(String serverCertPath, String serverKeyPath, String serverPass,
                                         String clientCertPath,
                                         SslProvider sslProvider) {
        try {
            File serverCert =  new File(serverCertPath);
            File serverKey = new File(serverKeyPath);
            File clientCert =  new File(clientCertPath);

            if (!serverCert.exists() || !serverKey.exists() || !clientCert.exists()) {
                log.warn("ATTENTION. Certificate {}, key {}, clietn cert {} paths not valid. Using embedded certs. This is not secure. Please replace it with your own certs.",
                        serverCert.getAbsolutePath(), serverKey.getAbsolutePath(), clientCert.getAbsolutePath());

                return new AppSslContext(false, build(sslProvider));
            }

            return new AppSslContext(true, build(serverCert, serverKey, serverPass, sslProvider, clientCert));
        } catch (CertificateException | SSLException | IllegalArgumentException e) {
            log.error("Error initializing ssl context. Reason : {}", e.getMessage());
            throw new RuntimeException(e.getMessage());
        }
    }

    public static SslContext build(SslProvider sslProvider) throws CertificateException, SSLException {
        SelfSignedCertificate ssc = new SelfSignedCertificate();
        return SslContextBuilder.forServer(ssc.certificate(), ssc.privateKey())
                .sslProvider(sslProvider)
                .build();
    }

    public static SslContext build(File serverCert, File serverKey, String serverPass, SslProvider sslProvider) throws SSLException {
        return SslContextBuilder.forServer(serverCert, serverKey, serverPass)
                .sslProvider(sslProvider)
                .build();
    }

    public static SslContext build(File serverCert, File serverKey, String serverPass, SslProvider sslProvider, File clientCert) throws SSLException {
        return SslContextBuilder.forServer(serverCert, serverKey, serverPass)
                .sslProvider(sslProvider)
                .trustManager(clientCert)
                .build();
    }

}
