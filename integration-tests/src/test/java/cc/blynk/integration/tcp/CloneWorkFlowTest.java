package cc.blynk.integration.tcp;

import cc.blynk.integration.IntegrationBase;
import cc.blynk.integration.model.tcp.ClientPair;
import cc.blynk.server.Holder;
import cc.blynk.server.core.model.DashBoard;
import cc.blynk.server.core.model.Profile;
import cc.blynk.server.core.model.device.Device;
import cc.blynk.server.core.model.serialization.JsonParser;
import cc.blynk.server.servers.BaseServer;
import cc.blynk.server.servers.application.AppAndHttpsServer;
import cc.blynk.server.servers.hardware.HardwareAndHttpAPIServer;
import cc.blynk.utils.StringUtils;
import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.DefaultAsyncHttpClient;
import org.asynchttpclient.DefaultAsyncHttpClientConfig;
import org.asynchttpclient.Response;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.concurrent.Future;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 2/2/2015.
 *
 */
@RunWith(MockitoJUnitRunner.class)
public class CloneWorkFlowTest extends IntegrationBase {

    private BaseServer appServer;
    private BaseServer httpServer;
    private ClientPair clientPair;

    @Before
    public void init() throws Exception {
        holder = new Holder(properties, twitterWrapper, mailWrapper,
                gcmWrapper, smsWrapper, slackWrapper, "db-test.properties");

        assertNotNull(holder.dbManager.getConnection());

        this.appServer = new AppAndHttpsServer(holder).start();
        this.httpServer = new HardwareAndHttpAPIServer(holder).start();

        this.clientPair = initAppAndHardPair();
        holder.dbManager.executeSQL("DELETE FROM cloned_projects");
    }

    @After
    public void shutdown() {
        this.appServer.close();
        this.httpServer.close();
        this.clientPair.stop();
    }

    @Test
    public void testGetNonExistingQR() throws Exception  {
        clientPair.appClient.send("getProjectByCloneCode " + 123);
        clientPair.appClient.verifyResult(serverError(1));
    }

    @Test
    public void testCloneForLocalServerWithNoDB() throws Exception  {
        holder.dbManager.close();

        clientPair.appClient.send("getCloneCode 1");
        String token = clientPair.appClient.getBody();
        assertNotNull(token);
        assertEquals(32, token.length());

        clientPair.appClient.send("getProjectByCloneCode " + token);
        DashBoard dashBoard = clientPair.appClient.getDash(2);
        assertEquals("My Dashboard", dashBoard.name);
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
        DashBoard dashBoard = clientPair.appClient.getDash(2);
        assertEquals("My Dashboard", dashBoard.name);
        Device device = dashBoard.devices[0];
        assertEquals(0, device.connectTime);
        assertEquals(0, device.dataReceivedAt);
        assertEquals(0, device.disconnectTime);
        assertEquals(0, device.firstConnectTime);
        assertNull(device.deviceOtaInfo);
        assertNull(device.hardwareInfo);

        clientPair.appClient.send("loadProfileGzipped");
        Profile profile = clientPair.appClient.getProfile(3);
        assertEquals(1, profile.dashBoards.length);
    }

    @Test
    public void getProjectByCloneCodeNewFormat() throws Exception {
        clientPair.appClient.send("getCloneCode 1");
        String token = clientPair.appClient.getBody();
        assertNotNull(token);
        assertEquals(32, token.length());

        clientPair.appClient.send("getProjectByCloneCode " + token + StringUtils.BODY_SEPARATOR_STRING + "new");
        DashBoard dashBoard = clientPair.appClient.getDash(2);
        assertEquals("My Dashboard", dashBoard.name);

        clientPair.appClient.send("loadProfileGzipped");
        Profile profile = clientPair.appClient.getProfile(3);
        assertEquals(2, profile.dashBoards.length);
        assertEquals(2, profile.dashBoards[1].id);
    }

    @Test
    public void getProjectByCloneCodeViaHttp() throws Exception {
        clientPair.appClient.send("getCloneCode 1");
        String token = clientPair.appClient.getBody();
        assertNotNull(token);
        assertEquals(32, token.length());

        AsyncHttpClient httpclient = new DefaultAsyncHttpClient(
                new DefaultAsyncHttpClientConfig.Builder()
                        .setUserAgent(null)
                        .setKeepAlive(true)
                        .build()
        );

        Future<Response> f = httpclient.prepareGet("http://localhost:" + httpPort + "/" + token + "/clone").execute();
        Response response = f.get();
        assertEquals(200, response.getStatusCode());
        String responseBody = response.getResponseBody();
        assertNotNull(responseBody);
        DashBoard dashBoard = JsonParser.parseDashboard(responseBody, 0);
        assertEquals("My Dashboard", dashBoard.name);
    }

    @Test
    public void getProjectByNonExistingCloneCodeViaHttp() throws Exception {
        AsyncHttpClient httpclient = new DefaultAsyncHttpClient(
                new DefaultAsyncHttpClientConfig.Builder()
                        .setUserAgent(null)
                        .setKeepAlive(true)
                        .build()
        );

        Future<Response> f = httpclient.prepareGet("http://localhost:" + httpPort + "/" + 123 + "/clone").execute();
        Response response = f.get();
        assertEquals(500, response.getStatusCode());
        String responseBody = response.getResponseBody();
        assertEquals("Requested QR not found.", responseBody);
    }

}
