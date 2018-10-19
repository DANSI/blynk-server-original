package cc.blynk.integration.tcp;

import cc.blynk.integration.SingleServerInstancePerTest;
import cc.blynk.integration.model.tcp.TestAppClient;
import cc.blynk.integration.model.tcp.TestHardClient;
import cc.blynk.server.core.model.device.BoardType;
import cc.blynk.server.core.model.device.Device;
import cc.blynk.utils.AppNameUtil;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import static cc.blynk.integration.TestUtil.ok;
import static org.junit.Assert.assertNotNull;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 2/2/2015.
 *
 */
@RunWith(MockitoJUnitRunner.class)
public class PortUnificationTest extends SingleServerInstancePerTest {

    @Test
    public void testAppConectsOk() throws Exception {
        int appPort = properties.getHttpsPort();
        TestAppClient appClient = new TestAppClient("localhost", appPort, properties);
        appClient.start();

        appClient.register(incrementAndGetUserName(), "1", AppNameUtil.BLYNK);
        appClient.login(getUserName(), "1", "Android", "1.10.4");
        appClient.verifyResult(ok(1));
        appClient.verifyResult(ok(2));
    }

    @Test
    public void testHardwareConnectsOk() throws Exception {
        int appPort = properties.getHttpsPort();
        TestAppClient appClient = new TestAppClient("localhost", appPort, properties);
        appClient.start();

        appClient.register(incrementAndGetUserName(), "1", AppNameUtil.BLYNK);
        appClient.login(getUserName(), "1", "Android", "1.10.4");
        appClient.createDash("{\"id\":1, \"createdAt\":1, \"name\":\"test board\"}");

        Device device1 = new Device(1, "My Device", BoardType.ESP8266);
        appClient.createDevice(1, device1);

        appClient.verifyResult(ok(1));
        appClient.verifyResult(ok(2));
        appClient.verifyResult(ok(3));

        Device device = appClient.parseDevice(4);
        assertNotNull(device);
        assertNotNull(device.token);

        int hardwarePort = properties.getHttpPort();
        TestHardClient hardClient = new TestHardClient("localhost", hardwarePort);
        hardClient.start();

        hardClient.login(device.token);
        hardClient.verifyResult(ok(1));
    }
}
