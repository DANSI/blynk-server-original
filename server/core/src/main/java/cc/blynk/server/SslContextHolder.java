package cc.blynk.server;

import cc.blynk.server.acme.AcmeClient;
import cc.blynk.server.acme.ContentHolder;
import cc.blynk.utils.properties.ServerProperties;
import io.netty.handler.ssl.OpenSsl;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SslProvider;
import io.netty.handler.ssl.util.SelfSignedCertificate;
import io.netty.util.internal.PlatformDependent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.net.ssl.SSLException;
import java.io.File;
import java.security.cert.CertificateException;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 30.04.17.
 */
public class SslContextHolder {

    private static final Logger log = LogManager.getLogger(SslContextHolder.class);

    public volatile SslContext sslCtx;

    public final AcmeClient acmeClient;

    private final boolean isAutoGenerationEnabled;

    public final boolean isNeedInitializeOnStart;

    public final ContentHolder contentHolder;

    private final boolean onlyLatestTLS;

    SslContextHolder(ServerProperties props, String email) {
        this.contentHolder = new ContentHolder();
        this.onlyLatestTLS = props.getBoolProperty("latest.tls");

        String certPath = props.getProperty("server.ssl.cert");
        String keyPath = props.getProperty("server.ssl.key");
        String keyPass = props.getProperty("server.ssl.key.pass");

        if (certPath == null || certPath.isEmpty()) {
            log.info("Didn't find custom user certificates.");
            isAutoGenerationEnabled = true;
        } else {
            isAutoGenerationEnabled = false;
        }

        String host = props.getProperty("server.host");
        if (AcmeClient.DOMAIN_CHAIN_FILE.exists() && AcmeClient.DOMAIN_KEY_FILE.exists()) {
            log.info("Found generated with Let's Encrypt certificates.");

            certPath = AcmeClient.DOMAIN_CHAIN_FILE.getAbsolutePath();
            keyPath = AcmeClient.DOMAIN_KEY_FILE.getAbsolutePath();
            keyPass = null;

            this.isNeedInitializeOnStart = false;
            this.acmeClient = new AcmeClient(email, host, contentHolder);
        } else {
            log.info("Didn't find Let's Encrypt certificates.");
            if (host == null || host.isEmpty() || email == null || email.isEmpty()
                    || email.equals("example@gmail.com") || email.startsWith("SMTP")) {
                log.warn("You didn't specified 'server.host' or 'contact.email' "
                        + "properties in server.properties file. "
                        + "Automatic certificate generation is turned off. "
                        + "Please specify above properties for automatic certificates retrieval.");
                this.acmeClient = null;
                this.isNeedInitializeOnStart = false;
            } else {
                log.info("Automatic certificate generation is turned ON.");
                this.acmeClient = new AcmeClient(email, host, contentHolder);
                this.isNeedInitializeOnStart = true;
            }
        }

        if (isOpenSslAvailable()) {
            log.info("Using native openSSL provider.");
        }
        SslProvider sslProvider = fetchSslProvider();
        this.sslCtx = initSslContext(certPath, keyPath, keyPass, sslProvider);
    }

    static boolean isOpenSslAvailable() {
        return PlatformDependent.bitMode() != 32 && OpenSsl.isAvailable();
    }

    public void regenerate() throws Exception {
        this.acmeClient.requestCertificate();

        String certPath = AcmeClient.DOMAIN_CHAIN_FILE.getAbsolutePath();
        String keyPath = AcmeClient.DOMAIN_KEY_FILE.getAbsolutePath();

        SslProvider sslProvider = fetchSslProvider();
        this.sslCtx = initSslContext(certPath, keyPath, null, sslProvider);
    }

    public boolean runRenewalWorker() {
        return isAutoGenerationEnabled && acmeClient != null;
    }

    public void generateInitialCertificates(ServerProperties props) {
        if (isAutoGenerationEnabled && isNeedInitializeOnStart) {
            System.out.println("Generating own initial certificates...");
            try {
                regenerate();
                System.out.println("Success! The certificate for your domain "
                        + props.getProperty("server.host") + " has been generated!");
            } catch (Exception e) {
                System.out.println("Error during certificate generation.");
                System.out.println(e.getMessage());
            }
        }
    }

    private SslContext initSslContext(String serverCertPath, String serverKeyPath, String serverPass,
                                      SslProvider sslProvider) {
        try {
            File serverCert = new File(serverCertPath);
            File serverKey = new File(serverKeyPath);

            if (!serverCert.exists() || !serverKey.exists()) {
                log.warn("ATTENTION. Server certificate paths (cert : '{}', key : '{}') not valid."
                                + " Using embedded server certs and one way ssl. This is not secure."
                                + " Please replace it with your own certs.",
                        serverCert.getAbsolutePath(), serverKey.getAbsolutePath());

                return build(sslProvider);
            }

            return build(serverCert, serverKey, serverPass, sslProvider);
        } catch (CertificateException | SSLException | IllegalArgumentException e) {
            log.error("Error initializing ssl context. Reason : {}", e.getMessage());
            throw new RuntimeException(e.getMessage());
        }
    }

    private static SslProvider fetchSslProvider() {
        return isOpenSslAvailable() ? SslProvider.OPENSSL : SslProvider.JDK;
    }

    public static SslContext build(SslProvider sslProvider) throws CertificateException, SSLException {
        SelfSignedCertificate ssc = new SelfSignedCertificate();
        return SslContextBuilder.forServer(ssc.certificate(), ssc.privateKey())
                .sslProvider(sslProvider)
                .build();
    }

    public SslContext build(File serverCert, File serverKey,
                            String serverPass, SslProvider sslProvider) throws SSLException {
        SslContextBuilder sslContextBuilder;
        if (serverPass == null || serverPass.isEmpty()) {
            sslContextBuilder = SslContextBuilder.forServer(serverCert, serverKey)
                    .sslProvider(sslProvider);
        } else {
            sslContextBuilder = SslContextBuilder.forServer(serverCert, serverKey, serverPass)
                    .sslProvider(sslProvider);
        }
        if (this.onlyLatestTLS) {
            sslContextBuilder.protocols("TLSv1.3", "TLSv1.2");
        }
        return sslContextBuilder.build();
    }

    public static SslContext build(File serverCert, File serverKey, String serverPass,
                                   SslProvider sslProvider, File clientCert) throws SSLException {
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
