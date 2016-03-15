package cc.blynk.utils;

import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslProvider;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.net.ssl.SSLException;
import java.io.File;
import java.security.cert.CertificateException;

import static cc.blynk.utils.SslUtil.*;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 05.01.16.
 */
public class AppSslUtil {

    private final static Logger log = LogManager.getLogger(AppSslUtil.class);

    public static SslContext initMutualSslContext(ServerProperties props) {
        SslProvider sslProvider = fetchSslProvider(props);

        return initMutualSslContext(
                props.getProperty("server.ssl.cert"),
                props.getProperty("server.ssl.key"),
                props.getProperty("server.ssl.key.pass"),
                props.getProperty("client.ssl.cert"),
                sslProvider);
    }

    private static SslContext initMutualSslContext(String serverCertPath, String serverKeyPath, String serverPass,
                                                      String clientCertPath,
                                                      SslProvider sslProvider) {
        try {
            File serverCert = new File(serverCertPath);
            File serverKey = new File(serverKeyPath);
            File clientCert = new File(clientCertPath);

            if (!serverCert.exists() || !serverKey.exists()) {
                log.warn("ATTENTION. Server certificate paths cert : '{}', key : '{}' - not valid. Using embedded server certs and one way ssl. This is not secure. Please replace it with your own certs.",
                        serverCert.getAbsolutePath(), serverKey.getAbsolutePath());

                return build(sslProvider);
            }

            if (!clientCert.exists()) {
                log.warn("Found server certificates but no client certificate for '{}' path. Using one way ssl.", clientCert.getAbsolutePath());

                return build(serverCert, serverKey, serverPass, sslProvider);
            }

            return build(serverCert, serverKey, serverPass, sslProvider, clientCert);
        } catch (CertificateException | SSLException | IllegalArgumentException e) {
            log.error("Error initializing ssl context. Reason : {}", e.getMessage());
            throw new RuntimeException(e.getMessage());
        }
    }

}
