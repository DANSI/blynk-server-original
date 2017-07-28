package cc.blynk.integration.tcp;

import cc.blynk.integration.IntegrationBase;
import cc.blynk.integration.model.tcp.ClientPair;
import cc.blynk.server.Holder;
import cc.blynk.server.application.AppServer;
import cc.blynk.server.core.BaseServer;
import cc.blynk.server.core.model.DashBoard;
import cc.blynk.server.hardware.HardwareServer;
import cc.blynk.utils.JsonParser;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 2/2/2015.
 *
 */
@RunWith(MockitoJUnitRunner.class)
public class CloneWorkFlowTest extends IntegrationBase {

    private BaseServer appServer;
    private BaseServer hardwareServer;
    private ClientPair clientPair;

    @Before
    public void init() throws Exception {
        holder = new Holder(properties, twitterWrapper, mailWrapper, gcmWrapper, smsWrapper, "db-test.properties");

        assertNotNull(holder.dbManager.getConnection());

        this.hardwareServer = new HardwareServer(holder).start();
        this.appServer = new AppServer(holder).start();

        this.clientPair = initAppAndHardPair();
        holder.dbManager.executeSQL("DELETE FROM cloned_projects");
    }

    @After
    public void shutdown() {
        this.appServer.close();
        this.hardwareServer.close();
        this.clientPair.stop();
    }

    @Test
    public void getCloneCode() throws Exception {
        clientPair.appClient.send("getCloneCode 1");
        String token = clientPair.appClient.getBody();
        assertNotNull(token);
        assertEquals(32, token.length());
    }

    @Test
    public void getProjectByCloneCode() throws Exception {
        clientPair.appClient.send("getCloneCode 1");
        String token = clientPair.appClient.getBody();
        assertNotNull(token);
        assertEquals(32, token.length());

        clientPair.appClient.send("getProjectByCloneCode " + token);
        String dashJson = clientPair.appClient.getBody(2);
        assertNotNull(dashJson);
        DashBoard dashBoard = JsonParser.parseDashboard(dashJson);
        assertEquals("My Dashboard", dashBoard.name);
    }

}
