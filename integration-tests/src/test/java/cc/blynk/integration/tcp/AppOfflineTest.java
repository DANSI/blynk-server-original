package cc.blynk.integration.tcp;

import cc.blynk.integration.BaseTest;
import cc.blynk.integration.TestUtil;
import cc.blynk.integration.model.tcp.ClientPair;
import cc.blynk.server.Holder;
import cc.blynk.server.servers.BaseServer;
import cc.blynk.server.servers.application.MobileAndHttpsServer;
import cc.blynk.server.servers.hardware.HardwareAndHttpAPIServer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import static cc.blynk.integration.TestUtil.createDefaultHolder;
import static cc.blynk.integration.TestUtil.internal;
import static cc.blynk.integration.TestUtil.ok;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 2/2/2015.
 *
 */
@RunWith(MockitoJUnitRunner.class)
public class AppOfflineTest extends BaseTest {

    private BaseServer appServer;
    private BaseServer hardwareServer;
    private ClientPair clientPair;

    @Before
    public void init() throws Exception {
        properties.setProperty("app.socket.idle.timeout", "1");
        Holder holder = createDefaultHolder(properties, "no-db.properties");
        this.hardwareServer = new HardwareAndHttpAPIServer(holder).start();
        this.appServer = new MobileAndHttpsServer(holder).start();

        this.clientPair = initAppAndHardPair("user_profile_json_empty_dash.txt");
    }

    @After
    public void shutdown() {
        this.appServer.close();
        this.hardwareServer.close();
        this.clientPair.stop();
    }

    @Test
    public void testWarn() throws Exception {
        clientPair.appClient.updateDash("{\"id\":1, \"name\":\"test board\", \"isAppConnectedOn\":true}");
        clientPair.appClient.verifyResult(ok(1));

        TestUtil.sleep(1500);

        clientPair.hardwareClient.verifyResult(internal(7777, "adis"));
    }

}
