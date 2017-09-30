package cc.blynk.server;

import cc.blynk.server.acme.AcmeClient;
import cc.blynk.server.acme.ContentHolder;
import cc.blynk.utils.SslUtil;
import cc.blynk.utils.properties.ServerProperties;
import io.netty.handler.ssl.OpenSsl;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslProvider;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 30.04.17.
 */
public class SslContextHolder {

    private static final Logger log = LogManager.getLogger(SslContextHolder.class);

    public volatile SslContext sslCtx;

    public final AcmeClient acmeClient;

    public final boolean isAutoGenerationEnabled;

    public final boolean isNeedInitializeOnStart;

    public final ContentHolder contentHolder;

    SslContextHolder(ServerProperties props, String email) {
        this.contentHolder = new ContentHolder();

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

        if (OpenSsl.isAvailable()) {
            log.info("Using native openSSL provider.");
        }
        SslProvider sslProvider = SslUtil.fetchSslProvider();
        this.sslCtx = SslUtil.initSslContext(certPath, keyPath, keyPass, sslProvider, true);
    }

    private void regenerate() {
        String certPath = AcmeClient.DOMAIN_CHAIN_FILE.getAbsolutePath();
        String keyPath = AcmeClient.DOMAIN_KEY_FILE.getAbsolutePath();

        SslProvider sslProvider = SslUtil.fetchSslProvider();
        this.sslCtx = SslUtil.initSslContext(certPath, keyPath, null, sslProvider, true);
    }

    public void generateInitialCertificates(ServerProperties props) {
        if (isAutoGenerationEnabled && isNeedInitializeOnStart) {
            System.out.println("Generating own initial certificates...");
            try {
                if (this.acmeClient.requestCertificate()) {
                    System.out.println("Success! The certificate for your domain "
                            + props.getProperty("server.host") + " has been generated!");
                    regenerate();
                }
            } catch (Exception e) {
                System.out.println("Error during certificate generation.");
                System.out.println(e.getMessage());
            }
        }
    }

}
