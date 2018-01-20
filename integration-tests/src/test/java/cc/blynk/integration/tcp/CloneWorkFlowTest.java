package cc.blynk.integration.tcp;

import cc.blynk.integration.IntegrationBase;
import cc.blynk.integration.model.tcp.ClientPair;
import cc.blynk.server.Holder;
import cc.blynk.server.api.http.AppAndHttpsServer;
import cc.blynk.server.api.http.HardwareAndHttpAPIServer;
import cc.blynk.server.core.BaseServer;
import cc.blynk.server.core.model.DashBoard;
import cc.blynk.server.core.model.serialization.JsonParser;
import cc.blynk.server.hardware.HardwareServer;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

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
    private BaseServer httpServer;
    private ClientPair clientPair;

    @Before
    public void init() throws Exception {
        holder = new Holder(properties, twitterWrapper, mailWrapper, gcmWrapper, smsWrapper, "db-test.properties");

        assertNotNull(holder.dbManager.getConnection());

        this.hardwareServer = new HardwareServer(holder).start();
        this.appServer = new AppAndHttpsServer(holder).start();
        this.httpServer = new HardwareAndHttpAPIServer(holder).start();

        this.clientPair = initAppAndHardPair();
        holder.dbManager.executeSQL("DELETE FROM cloned_projects");
    }

    @After
    public void shutdown() {
        this.appServer.close();
        this.hardwareServer.close();
        this.httpServer.close();
        this.clientPair.stop();
    }

    @Test
    public void testGetNonExistingQR() throws Exception  {
        clientPair.appClient.send("getProjectByCloneCode " + 123);
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(serverError(1)));
    }

    @Test
    public void testCloneForLocalServerWithNoDB() throws Exception  {
        holder.dbManager.close();

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
        DashBoard dashBoard = JsonParser.parseDashboard(responseBody);
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
