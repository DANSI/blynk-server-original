package cc.blynk.server.workers;

import cc.blynk.server.acme.AcmeClient;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.shredzone.acme4j.util.CertificateUtils;

import java.io.FileInputStream;
import java.security.cert.X509Certificate;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 01.05.17.
 */
public class CertificateRenewalWorker implements Runnable {

    private static final Logger log = LogManager.getLogger(CertificateRenewalWorker.class);

    private final AcmeClient acmeClient;
    private final int renewBeforeDays;

    public CertificateRenewalWorker(AcmeClient acmeClient, int renewBeforeDays) {
        this.acmeClient = acmeClient;
        this.renewBeforeDays = renewBeforeDays;
    }

    @Override
    public void run() {
        try {
            if (AcmeClient.DOMAIN_CHAIN_FILE.exists()) {
                //stream closed inside utilities method
                X509Certificate cert = CertificateUtils.readX509Certificate(new FileInputStream(AcmeClient.DOMAIN_CHAIN_FILE));

                Date expirationDate = cert.getNotAfter();
                log.info("Certificate expiration date is {}. Days left : {}", expirationDate, getDateDiff(expirationDate));

                //certificate will expire in 1 week
                Date oneWeekAheadDate = getNowDatePlusDays(renewBeforeDays);
                if (expirationDate.before(oneWeekAheadDate)) {
                    log.warn("Trying to renew...");
                    if (acmeClient.requestCertificate()) {
                        log.info("Success! The certificate for your domain has been renewed!");
                    }
                }
            } else {
                if (acmeClient.requestCertificate()) {
                    log.info("Success! The certificate for your domain has been renewed!");
                }
            }
        } catch (Exception e) {
            log.error("Error during certificate renewal.", e);
        }
    }

    private static long getDateDiff(Date date2) {
        long now = System.currentTimeMillis();
        return TimeUnit.MILLISECONDS.toDays(date2.getTime() - now);
    }

    private static Date getNowDatePlusDays(int days) {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_MONTH, days);
        return cal.getTime();
    }
}
