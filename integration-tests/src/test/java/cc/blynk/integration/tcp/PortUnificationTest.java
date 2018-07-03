package cc.blynk.integration.tcp;

import cc.blynk.integration.BaseTest;
import cc.blynk.integration.model.tcp.TestAppClient;
import cc.blynk.integration.model.tcp.TestHardClient;
import cc.blynk.server.core.model.device.Device;
import cc.blynk.server.servers.BaseServer;
import cc.blynk.server.servers.application.AppAndHttpsServer;
import cc.blynk.server.servers.hardware.HardwareAndHttpAPIServer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import static cc.blynk.integration.TestUtil.DEFAULT_TEST_USER;
import static cc.blynk.integration.TestUtil.ok;
import static org.junit.Assert.assertNotNull;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 2/2/2015.
 *
 */
@RunWith(MockitoJUnitRunner.class)
public class PortUnificationTest extends BaseTest {

    private BaseServer httpsAndAppServer;
    private BaseServer httpAndHardwareServer;

    @Before
    public void init() throws Exception {
        this.httpsAndAppServer = new AppAndHttpsServer(holder).start();
        this.httpAndHardwareServer = new HardwareAndHttpAPIServer(holder).start();
    }

    @After
    public void shutdown() {
        this.httpAndHardwareServer.close();
        this.httpsAndAppServer.close();
    }

    @Test
    public void testAppConectsOk() throws Exception {
        int appPort = httpsPort;
        TestAppClient appClient = new TestAppClient("localhost", appPort, properties);
        appClient.start();

        appClient.register(DEFAULT_TEST_USER, "1");
        appClient.login(DEFAULT_TEST_USER, "1", "Android", "1.10.4");
        appClient.verifyResult(ok(1));
        appClient.verifyResult(ok(2));
    }

    @Test
    public void testHardwareConnectsOk() throws Exception {
        int appPort = httpsPort;
        TestAppClient appClient = new TestAppClient("localhost", appPort, properties);
        appClient.start();

        appClient.register(DEFAULT_TEST_USER, "1");
        appClient.login(DEFAULT_TEST_USER, "1", "Android", "1.10.4");
        appClient.createDash("{\"id\":1, \"createdAt\":1, \"name\":\"test board\"}");

        Device device1 = new Device(1, "My Device", "ESP8266");
        appClient.createDevice(1, device1);

        appClient.verifyResult(ok(1));
        appClient.verifyResult(ok(2));
        appClient.verifyResult(ok(3));

        Device device = appClient.getDevice(4);
        assertNotNull(device);
        assertNotNull(device.token);

        int hardwarePort = httpPort;
        TestHardClient hardClient = new TestHardClient("localhost", hardwarePort);
        hardClient.start();

        hardClient.login(device.token);
        hardClient.verifyResult(ok(1));
    }
}
