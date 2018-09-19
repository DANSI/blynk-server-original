package cc.blynk.integration.http;

import cc.blynk.integration.BaseTest;
import cc.blynk.server.Holder;
import cc.blynk.server.SslContextHolder;
import cc.blynk.server.acme.AcmeClient;
import cc.blynk.server.acme.ContentHolder;
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

import static cc.blynk.integration.TestUtil.createDefaultHolder;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
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
        this.holder2 = createDefaultHolder(properties2, "no-db.properties");
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
        assertNull(contentHolder.content);
        AcmeClient acmeClient = new AcmeClient(STAGING, "test@blynk.cc", "let.blynk.cc", contentHolder);
        acmeClient.requestCertificate();
        assertNotNull(contentHolder.content);
    }

    @Test
    @Ignore
    public void testWorker() throws Exception {
        AcmeClient acmeClient = Mockito.mock(AcmeClient.class);
        SslContextHolder sslContextHolder = Mockito.mock(SslContextHolder.class);
        CertificateRenewalWorker certificateRenewalWorker = new CertificateRenewalWorker(sslContextHolder);
        certificateRenewalWorker.run();
        verify(acmeClient, times(0)).requestCertificate();

        certificateRenewalWorker = new CertificateRenewalWorker(sslContextHolder);
        certificateRenewalWorker.run();
        verify(acmeClient, times(1)).requestCertificate();
    }
}
