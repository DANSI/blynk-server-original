package cc.blynk.server.workers;

import cc.blynk.server.acme.AcmeClient;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.shredzone.acme4j.util.CertificateUtils;

import java.io.FileInputStream;
import java.security.cert.X509Certificate;
import java.util.Calendar;
import java.util.Date;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 01.05.17.
 */
public class CertificateRenewalWorker implements Runnable {

    private static final Logger log = LogManager.getLogger(CertificateRenewalWorker.class);

    private final AcmeClient acmeClient;

    public CertificateRenewalWorker(AcmeClient acmeClient) {
        this.acmeClient = acmeClient;
    }

    @Override
    public void run() {
        try {
            //stream closed inside utilities method
            X509Certificate cert = CertificateUtils.readX509Certificate(new FileInputStream(AcmeClient.DOMAIN_CHAIN_FILE));

            Date expirationDate = cert.getNotAfter();
            log.info("Certificate expiration date is {}", expirationDate);

            //certificate will expire in 1 week
            Date oneWeekAheadDate = getNowDatePlusDays(7);
            if (expirationDate.before(oneWeekAheadDate)) {
                log.warn("Certificate will expire in 1 week. Trying to renew...");
                if (acmeClient.requestCertificate()) {
                    log.info("Success! The certificate for your domain has been renewed!");
                }
            }
        } catch (Exception e) {
            log.error("Error during certificate renewal.", e);
        }
    }

    private static Date getNowDatePlusDays(int days) {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_MONTH, days);
        return cal.getTime();
    }
}
