package cc.blynk.server.workers;

import cc.blynk.server.SslContextHolder;
import cc.blynk.server.acme.AcmeClient;
import cc.blynk.utils.DateTimeUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.shredzone.acme4j.util.CertificateUtils;

import java.io.FileInputStream;
import java.security.cert.X509Certificate;
import java.time.LocalDate;
import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 01.05.17.
 */
public class CertificateRenewalWorker implements Runnable {

    private static final Logger log = LogManager.getLogger(CertificateRenewalWorker.class);

    private final SslContextHolder sslContextHolder;
    private final int renewBeforeDays;

    public CertificateRenewalWorker(SslContextHolder sslContextHolder, int renewBeforeDays) {
        this.sslContextHolder = sslContextHolder;
        this.renewBeforeDays = renewBeforeDays;
    }

    @Override
    public void run() {
        try {
            if (AcmeClient.DOMAIN_CHAIN_FILE.exists()) {
                //stream closed inside utilities method
                X509Certificate cert = CertificateUtils.readX509Certificate(
                        new FileInputStream(AcmeClient.DOMAIN_CHAIN_FILE));

                Date expirationDate = cert.getNotAfter();
                log.info("Certificate expiration date is {}. Days left : {}",
                        expirationDate, getDateDiff(expirationDate));

                //certificate will expire in 1 week
                LocalDate nowPlusRenewPeriod = LocalDate.now().plusDays(renewBeforeDays);
                Date aheadDate = Date.from(nowPlusRenewPeriod.atStartOfDay(DateTimeUtils.UTC).toInstant());
                if (expirationDate.before(aheadDate)) {
                    log.warn("Trying to renew...");
                    sslContextHolder.regenerate();
                    log.info("Success! The certificate for your domain has been renewed!");
                }
            } else {
                sslContextHolder.regenerate();
                log.info("Success! The certificate for your domain has been renewed!");
            }
        } catch (Exception e) {
            log.error("Error during certificate renewal.", e);
        }
    }

    private static long getDateDiff(Date date2) {
        long now = System.currentTimeMillis();
        return TimeUnit.MILLISECONDS.toDays(date2.getTime() - now);
    }

}
