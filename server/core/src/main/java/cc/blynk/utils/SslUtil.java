package cc.blynk.utils;

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

    public static SslContext initSslContext(String serverCertPath, String serverKeyPath, String serverPass,
                                                  SslProvider sslProvider, boolean printWarn) {
        try {
            File serverCert = new File(serverCertPath);
            File serverKey = new File(serverKeyPath);


            if (!serverCert.exists() || !serverKey.exists()) {
                if (printWarn) {
                    log.warn("ATTENTION. Server certificate paths (cert : '{}', key : '{}') not valid. Using embedded server certs and one way ssl. This is not secure. Please replace it with your own certs.",
                            serverCert.getAbsolutePath(), serverKey.getAbsolutePath());
                }

                return build(sslProvider);
            }

            return build(serverCert, serverKey, serverPass, sslProvider);
        } catch (CertificateException | SSLException | IllegalArgumentException e) {
            log.error("Error initializing ssl context. Reason : {}", e.getMessage());
            throw new RuntimeException(e.getMessage());
        }
    }

    public static SslProvider fetchSslProvider(ServerProperties props) {
        return props.getBoolProperty("enable.native.openssl") ? SslProvider.OPENSSL : SslProvider.JDK;
    }

    public static SslContext build(SslProvider sslProvider) throws CertificateException, SSLException {
        SelfSignedCertificate ssc = new SelfSignedCertificate();
        return SslContextBuilder.forServer(ssc.certificate(), ssc.privateKey())
                .sslProvider(sslProvider)
                .build();
    }

    public static SslContext build(File serverCert, File serverKey, String serverPass, SslProvider sslProvider) throws SSLException {
        if (serverPass == null || serverPass.isEmpty()) {
            return SslContextBuilder.forServer(serverCert, serverKey)
                    .sslProvider(sslProvider)
                    .build();
        } else {
            return SslContextBuilder.forServer(serverCert, serverKey, serverPass)
                    .sslProvider(sslProvider)
                    .build();
        }
    }

    public static SslContext build(File serverCert, File serverKey, String serverPass, SslProvider sslProvider, File clientCert) throws SSLException {
        log.info("Creating SSL context for cert '{}', key '{}', key pass '{}'",
                serverCert.getAbsolutePath(), serverKey.getAbsoluteFile(), serverPass);
        if (serverPass == null || serverPass.isEmpty()) {
            return SslContextBuilder.forServer(serverCert, serverKey)
                    .sslProvider(sslProvider)
                    .trustManager(clientCert)
                    .build();
        } else {
            return SslContextBuilder.forServer(serverCert, serverKey, serverPass)
                    .sslProvider(sslProvider)
                    .trustManager(clientCert)
                    .build();
        }
    }

}
