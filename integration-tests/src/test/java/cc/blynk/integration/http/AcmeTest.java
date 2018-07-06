package cc.blynk.integration.http;

import cc.blynk.integration.BaseTest;
import cc.blynk.server.Holder;
import cc.blynk.server.SslContextHolder;
import cc.blynk.server.acme.AcmeClient;
import cc.blynk.server.acme.ContentHolder;
import cc.blynk.server.core.SlackWrapper;
import cc.blynk.server.notifications.mail.MailWrapper;
import cc.blynk.server.notifications.push.GCMWrapper;
import cc.blynk.server.notifications.sms.SMSWrapper;
import cc.blynk.server.notifications.twitter.TwitterWrapper;
import cc.blynk.server.servers.BaseServer;
import cc.blynk.server.servers.hardware.HardwareAndHttpAPIServer;
import cc.blynk.server.workers.CertificateRenewalWorker;
import cc.blynk.utils.properties.ServerProperties;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Collections;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 07.01.16.
 */
@RunWith(MockitoJUnitRunner.class)
public class AcmeTest extends BaseTest {

    private BaseServer httpServer;
    private Holder holder2;

    @After
    public void shutdown() {
        httpServer.close();
    }

    @Before
    public void init() throws Exception {
        ServerProperties properties2 = new ServerProperties(Collections.emptyMap(), "no_certs.properties");
        this.holder2 = new Holder(properties2, mock(TwitterWrapper.class),
                mock(MailWrapper.class), mock(GCMWrapper.class),
                mock(SMSWrapper.class), mock(SlackWrapper.class),
                "no-db.properties");
        httpServer = new HardwareAndHttpAPIServer(holder2).start();
    }

    @Override
    public String getDataFolder() {
        return getRelativeDataFolder("/profiles");
    }

    @Test
    public void testCorrectContext() {
        SslContextHolder sslContextHolder = holder2.sslContextHolder;
        assertNotNull(sslContextHolder);
        assertTrue(sslContextHolder.runRenewalWorker());
        assertTrue(sslContextHolder.isNeedInitializeOnStart);
        assertNotNull(sslContextHolder.acmeClient);
    }

    @Test
    @Ignore
    public void testCreateCertificates() throws Exception {
        final String STAGING = "acme://letsencrypt.org/staging";
        ContentHolder contentHolder = holder2.sslContextHolder.contentHolder;
        AcmeClient acmeClient = new AcmeClient(STAGING, "test@blynk.cc", "test.blynk.cc", contentHolder);
        acmeClient.requestCertificate();
    }

    @Test
    @Ignore
    public void testWorker() throws Exception {
        AcmeClient acmeClient = Mockito.mock(AcmeClient.class);
        SslContextHolder sslContextHolder = Mockito.mock(SslContextHolder.class);
        CertificateRenewalWorker certificateRenewalWorker = new CertificateRenewalWorker(sslContextHolder, 7);
        certificateRenewalWorker.run();
        verify(acmeClient, times(0)).requestCertificate();

        certificateRenewalWorker = new CertificateRenewalWorker(sslContextHolder, 100);
        certificateRenewalWorker.run();
        verify(acmeClient, times(1)).requestCertificate();
    }
}
