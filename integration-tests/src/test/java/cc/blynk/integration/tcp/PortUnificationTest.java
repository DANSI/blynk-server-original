package cc.blynk.integration.tcp;

import cc.blynk.integration.IntegrationBase;
import cc.blynk.integration.model.tcp.TestAppClient;
import cc.blynk.server.api.http.HttpsAPIServer;
import cc.blynk.server.application.AppServer;
import cc.blynk.server.core.BaseServer;
import cc.blynk.server.hardware.HardwareServer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

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

    private BaseServer appServer;
    private BaseServer hardwareServer;
    private BaseServer httpAdminServer;

    @Before
    public void init() throws Exception {
        this.httpAdminServer = new HttpsAPIServer(holder).start();
        this.hardwareServer = new HardwareServer(holder).start();
        this.appServer = new AppServer(holder).start();
    }

    @After
    public void shutdown() {
        httpAdminServer.close();
        this.appServer.close();
        this.hardwareServer.close();
    }

    @Test
    public void testSendEmail() throws Exception {
        int port = httpsPort;
        TestAppClient appClient = new TestAppClient("localhost", port, properties);
        appClient.start();

        appClient.send("register " + DEFAULT_TEST_USER + " 1");
        appClient.send("login " + DEFAULT_TEST_USER + " 1" + " Android" + "\0" + "1.10.4");
        verify(appClient.responseMock, timeout(500)).channelRead(any(), eq(ok(1)));

        //appClient.send("email 1");
        //verify(mailWrapper, timeout(1000)).sendText(eq(DEFAULT_TEST_USER), eq("Auth Token for My Dashboard project and device My Device"), startsWith("Auth Token : "));
    }

}
