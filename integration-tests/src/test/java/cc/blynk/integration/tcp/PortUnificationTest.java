package cc.blynk.integration.tcp;

import cc.blynk.integration.IntegrationBase;
import cc.blynk.integration.model.tcp.TestAppClient;
import cc.blynk.integration.model.tcp.TestHardClient;
import cc.blynk.server.api.http.AppAndHttpsServer;
import cc.blynk.server.api.http.HardwareAndHttpAPIServer;
import cc.blynk.server.core.BaseServer;
import cc.blynk.server.core.model.device.Device;
import cc.blynk.server.core.model.serialization.JsonParser;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 2/2/2015.
 *
 */
@RunWith(MockitoJUnitRunner.class)
public class PortUnificationTest extends IntegrationBase {

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

        appClient.send("register " + DEFAULT_TEST_USER + " 1");
        appClient.send("login " + DEFAULT_TEST_USER + " 1" + " Android" + "\0" + "1.10.4");
        verify(appClient.responseMock, timeout(500)).channelRead(any(), eq(ok(1)));
        verify(appClient.responseMock, timeout(500)).channelRead(any(), eq(ok(2)));
    }

    @Test
    public void testHardwareConnectsOk() throws Exception {
        int appPort = httpsPort;
        TestAppClient appClient = new TestAppClient("localhost", appPort, properties);
        appClient.start();

        appClient.send("register " + DEFAULT_TEST_USER + " 1");
        appClient.send("login " + DEFAULT_TEST_USER + " 1" + " Android" + "\0" + "1.10.4");
        appClient.send("createDash {\"id\":1, \"createdAt\":1, \"name\":\"test board\"}\"");

        Device device1 = new Device(1, "My Device", "ESP8266");
        appClient.send("createDevice 1\0" + device1.toString());

        verify(appClient.responseMock, timeout(500)).channelRead(any(), eq(ok(1)));
        verify(appClient.responseMock, timeout(500)).channelRead(any(), eq(ok(2)));
        verify(appClient.responseMock, timeout(500)).channelRead(any(), eq(ok(3)));

        String createdDevice = appClient.getBody(4);
        Device device = JsonParser.parseDevice(createdDevice);
        assertNotNull(device);
        assertNotNull(device.token);

        int hardwarePort = httpPort;
        TestHardClient hardClient = new TestHardClient("localhost", hardwarePort);
        hardClient.start();

        hardClient.send("login " + device.token);
        verify(hardClient.responseMock, timeout(500)).channelRead(any(), eq(ok(1)));
    }
}
