package cc.blynk.integration.tcp;

import cc.blynk.integration.SingleServerInstancePerTestWithDB;
import cc.blynk.server.core.model.DashBoard;
import cc.blynk.server.core.model.Profile;
import cc.blynk.server.core.model.device.Device;
import cc.blynk.server.core.model.serialization.JsonParser;
import cc.blynk.server.core.model.widgets.controls.Slider;
import cc.blynk.utils.StringUtils;
import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.DefaultAsyncHttpClient;
import org.asynchttpclient.DefaultAsyncHttpClientConfig;
import org.asynchttpclient.Response;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.concurrent.Future;

import static cc.blynk.integration.TestUtil.serverError;
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
public class CloneWorkFlowTest extends SingleServerInstancePerTestWithDB {

    @Before
    public void deleteTable() throws Exception {
        holder.dbManager.executeSQL("DELETE FROM cloned_projects");
    }

    @Test
    public void testGetNonExistingQR() throws Exception  {
        clientPair.appClient.send("getProjectByCloneCode " + 123);
        clientPair.appClient.verifyResult(serverError(1));
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
        clientPair.hardwareClient.send("hardware vw 4 4");
        clientPair.hardwareClient.send("hardware vw 44 44");

        clientPair.appClient.send("getCloneCode 1");
        String token = clientPair.appClient.getBody(3);
        assertNotNull(token);
        assertEquals(32, token.length());

        clientPair.appClient.send("getProjectByCloneCode " + token);
        DashBoard dashBoard = clientPair.appClient.parseDash(4);
        assertEquals("My Dashboard", dashBoard.name);
        Device device = dashBoard.devices[0];
        assertEquals(0, device.connectTime);
        assertEquals(0, device.dataReceivedAt);
        assertEquals(0, device.disconnectTime);
        assertEquals(0, device.firstConnectTime);
        assertNull(device.deviceOtaInfo);
        assertNull(device.hardwareInfo);
        Slider slider = (Slider) dashBoard.getWidgetById(4);
        assertNotNull(slider);
        assertNull(slider.value);
        assertNotNull(dashBoard.pinsStorage);
        assertEquals(0, dashBoard.pinsStorage.size());

        clientPair.appClient.send("loadProfileGzipped");
        Profile profile = clientPair.appClient.parseProfile(5);
        assertEquals(1, profile.dashBoards.length);
    }

    @Test
    public void getProjectByCloneCodeNew() throws Exception {
        clientPair.appClient.send("getCloneCode 1");
        String token = clientPair.appClient.getBody();
        assertNotNull(token);
        assertEquals(32, token.length());

        clientPair.appClient.send("getProjectByCloneCode " + token + "\0" + "new");
        DashBoard dashBoard = clientPair.appClient.parseDash(2);
        assertEquals("My Dashboard", dashBoard.name);
        Device device = dashBoard.devices[0];
        assertEquals(-1, dashBoard.parentId);
        assertEquals(2, dashBoard.id);
        assertEquals(0, device.connectTime);
        assertEquals(0, device.dataReceivedAt);
        assertEquals(0, device.disconnectTime);
        assertEquals(0, device.firstConnectTime);
        assertNull(device.deviceOtaInfo);
        assertNull(device.hardwareInfo);
        assertNotNull(device.token);

        clientPair.appClient.send("loadProfileGzipped");
        Profile profile = clientPair.appClient.parseProfile(3);
        assertEquals(2, profile.dashBoards.length);
    }

    @Test
    public void getProjectByCloneCodeNewFormat() throws Exception {
        clientPair.appClient.send("getCloneCode 1");
        String token = clientPair.appClient.getBody();
        assertNotNull(token);
        assertEquals(32, token.length());

        clientPair.appClient.send("getProjectByCloneCode " + token + StringUtils.BODY_SEPARATOR_STRING + "new");
        DashBoard dashBoard = clientPair.appClient.parseDash(2);
        assertEquals("My Dashboard", dashBoard.name);

        clientPair.appClient.send("loadProfileGzipped");
        Profile profile = clientPair.appClient.parseProfile(3);
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

        Future<Response> f = httpclient.prepareGet("http://localhost:" + properties.getHttpPort() + "/" + token + "/clone").execute();
        Response response = f.get();
        assertEquals(200, response.getStatusCode());
        String responseBody = response.getResponseBody();
        assertNotNull(responseBody);
        DashBoard dashBoard = JsonParser.parseDashboard(responseBody, 0);
        assertEquals("My Dashboard", dashBoard.name);
        httpclient.close();
    }

    @Test
    public void getProjectByNonExistingCloneCodeViaHttp() throws Exception {
        AsyncHttpClient httpclient = new DefaultAsyncHttpClient(
                new DefaultAsyncHttpClientConfig.Builder()
                        .setUserAgent(null)
                        .setKeepAlive(true)
                        .build()
        );

        Future<Response> f = httpclient.prepareGet("http://localhost:" + properties.getHttpPort() + "/" + 123 + "/clone").execute();
        Response response = f.get();
        assertEquals(500, response.getStatusCode());
        String responseBody = response.getResponseBody();
        assertEquals("Requested QR not found.", responseBody);
        httpclient.close();
    }

}
