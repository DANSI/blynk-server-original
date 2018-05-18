package cc.blynk.integration.http;

import cc.blynk.integration.BaseTest;
import cc.blynk.server.core.dao.FileManager;
import cc.blynk.server.core.model.DashBoard;
import cc.blynk.server.core.model.auth.User;
import cc.blynk.server.core.model.device.Device;
import cc.blynk.server.core.model.serialization.JsonParser;
import cc.blynk.server.core.model.widgets.notifications.Notification;
import cc.blynk.server.servers.BaseServer;
import cc.blynk.server.servers.hardware.HardwareAndHttpAPIServer;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.InputStream;

import static org.junit.Assert.assertEquals;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 24.12.15.
 */
@RunWith(MockitoJUnitRunner.class)
public class HttpAPIProjectTest extends BaseTest {

    private static BaseServer httpServer;
    private static CloseableHttpClient httpclient;
    private static String httpsServerUrl;
    private DashBoard dashBoard;

    @AfterClass
    public static void shutdown() throws Exception {
        httpclient.close();
        httpServer.close();
    }

    @Before
    public void init() throws Exception {
        if (httpServer == null) {
            httpServer = new HardwareAndHttpAPIServer(holder).start();
            httpsServerUrl = String.format("http://localhost:%s/", httpPort);
            httpclient = HttpClients.createDefault();
        }
    }

    @Override
    public String getDataFolder() {
        return getRelativeDataFolder("/profiles");
    }

    //----------------------------GET METHODS SECTION

    @Test
    public void testGetWithFakeToken() throws Exception {
        String token = "4ae3851817194e2596cf1b7103603ef8";
        HttpGet request = new HttpGet(httpsServerUrl + token + "/project");

        InputStream is = getClass().getResourceAsStream("/profiles/u_dmitriy@blynk.cc.user");
        User user = JsonParser.MAPPER.readValue(is, User.class);
        new FileManager(null, "127.0.0.1").makeProfileChanges(user);

        Integer dashId = 125564119;
        dashBoard = user.profile.getDashById(dashId);

        //cleanup of sensetive data
        Notification notification = dashBoard.getNotificationWidget();
        notification.iOSTokens = null;
        notification.androidTokens = null;
        for (Device device : dashBoard.devices) {
            device.token = null;
        }

        try (CloseableHttpResponse response = httpclient.execute(request)) {
            assertEquals(200, response.getStatusLine().getStatusCode());
            //assertEquals(dashBoard.toString(), consumeText(response));

            assertEquals("{\"id\":125564119,\"parentId\":-1,\"isPreview\":false,\"name\":\"New Project\",\"createdAt\":0,\"updatedAt\":0,\"widgets\":[{\"type\":\"TWO_AXIS_JOYSTICK\",\"id\":1036300912,\"x\":0,\"y\":4,\"color\":-308477697,\"width\":5,\"height\":4,\"tabId\":0,\"isDefaultColor\":false,\"deviceId\":0,\"split\":true,\"autoReturnOn\":true,\"portraitLocked\":false,\"frequency\":0,\"pins\":[{\"pin\":15,\"pwmMode\":false,\"rangeMappingOn\":false,\"pinType\":\"ANALOG\",\"value\":\"1\",\"min\":0.0,\"max\":255.0},{\"pin\":15,\"pwmMode\":false,\"rangeMappingOn\":false,\"pinType\":\"ANALOG\",\"value\":\"2\",\"min\":0.0,\"max\":255.0}]},{\"type\":\"BUTTON\",\"id\":1036300899,\"x\":5,\"y\":0,\"color\":616861439,\"width\":2,\"height\":2,\"tabId\":0,\"isDefaultColor\":false,\"deviceId\":0,\"pinType\":\"DIGITAL\",\"pin\":8,\"pwmMode\":false,\"rangeMappingOn\":false,\"min\":0.0,\"max\":0.0,\"value\":\"0\",\"pushMode\":true},{\"type\":\"LED\",\"id\":1036300908,\"x\":4,\"y\":0,\"color\":1602017535,\"width\":1,\"height\":1,\"tabId\":0,\"label\":\"gggg\",\"isDefaultColor\":false,\"deviceId\":0,\"pin\":-1,\"pwmMode\":false,\"rangeMappingOn\":false,\"min\":0.0,\"max\":0.0},{\"type\":\"LED\",\"id\":1036300904,\"x\":7,\"y\":0,\"color\":1602017535,\"width\":1,\"height\":1,\"tabId\":0,\"isDefaultColor\":false,\"deviceId\":0,\"pinType\":\"VIRTUAL\",\"pin\":2,\"pwmMode\":false,\"rangeMappingOn\":false,\"min\":0.0,\"max\":0.0},{\"type\":\"TIMER\",\"id\":276297114,\"x\":0,\"y\":1,\"color\":-308477697,\"width\":3,\"height\":1,\"tabId\":0,\"isDefaultColor\":false,\"deviceId\":0,\"pinType\":\"DIGITAL\",\"pin\":0,\"pwmMode\":false,\"rangeMappingOn\":false,\"min\":0.0,\"max\":0.0,\"value\":\"1\",\"startTime\":75600,\"startValue\":\"1\",\"stopTime\":75599,\"stopValue\":\"0\"},{\"type\":\"LED\",\"id\":1036300903,\"x\":4,\"y\":1,\"color\":1602017535,\"width\":1,\"height\":1,\"tabId\":0,\"isDefaultColor\":false,\"deviceId\":0,\"pinType\":\"VIRTUAL\",\"pin\":1,\"pwmMode\":false,\"rangeMappingOn\":false,\"min\":0.0,\"max\":0.0},{\"type\":\"SLIDER\",\"id\":1036300911,\"x\":0,\"y\":0,\"color\":-308477697,\"width\":4,\"height\":1,\"tabId\":0,\"isDefaultColor\":false,\"deviceId\":0,\"pinType\":\"DIGITAL\",\"pin\":3,\"pwmMode\":true,\"rangeMappingOn\":false,\"min\":0.0,\"max\":255.0,\"value\":\"87\",\"sendOnReleaseOn\":false,\"frequency\":0,\"maximumFractionDigits\":0,\"showValueOn\":true},{\"type\":\"GRAPH\",\"id\":1036300905,\"x\":0,\"y\":2,\"color\":-308477697,\"width\":8,\"height\":2,\"tabId\":0,\"isDefaultColor\":false,\"deviceId\":0,\"pinType\":\"VIRTUAL\",\"pin\":5,\"pwmMode\":false,\"rangeMappingOn\":false,\"min\":0.0,\"max\":1023.0,\"frequency\":0,\"isBar\":true},{\"type\":\"LED\",\"id\":1036300902,\"x\":3,\"y\":1,\"color\":1602017535,\"width\":1,\"height\":1,\"tabId\":0,\"isDefaultColor\":false,\"deviceId\":0,\"pinType\":\"VIRTUAL\",\"pin\":0,\"pwmMode\":false,\"rangeMappingOn\":false,\"min\":0.0,\"max\":0.0},{\"type\":\"DIGIT4_DISPLAY\",\"id\":1036300913,\"x\":5,\"y\":4,\"color\":-308477697,\"width\":2,\"height\":1,\"tabId\":0,\"isDefaultColor\":false,\"deviceId\":0,\"pinType\":\"ANALOG\",\"pin\":14,\"pwmMode\":false,\"rangeMappingOn\":false,\"min\":0.0,\"max\":1023.0,\"frequency\":1000},{\"type\":\"BUTTON\",\"id\":1036300914,\"x\":5,\"y\":5,\"color\":616861439,\"width\":2,\"height\":2,\"tabId\":0,\"isDefaultColor\":false,\"deviceId\":0,\"pinType\":\"DIGITAL\",\"pin\":1,\"pwmMode\":false,\"rangeMappingOn\":false,\"min\":0.0,\"max\":0.0,\"value\":\"1\",\"pushMode\":false},{\"type\":\"NOTIFICATION\",\"id\":1036300915,\"x\":5,\"y\":7,\"color\":0,\"width\":2,\"height\":1,\"tabId\":0,\"isDefaultColor\":false,\"androidTokens\":{\"98b68199-b394-4d46-81c0-6fd077000f80\":\"cu21UsfOTz0:APA91bFUWdcSSXg3w3Q4rSYxA_zeJQoBJGKs_2X0yHgRHEZ9wF32wMyPzutSwRbJk6OeoeATalVUmJbyNiXi4jjOFH8aDc4DX66pexjbQU2MkWqWdtWCCNgk4xooSyKN4ALbSVL-5ABm\"},\"notifyWhenOffline\":false,\"notifyWhenOfflineIgnorePeriod\":0,\"priority\":\"normal\"},{\"type\":\"EMAIL\",\"id\":1036300916,\"x\":5,\"y\":9,\"color\":0,\"width\":2,\"height\":1,\"tabId\":0,\"isDefaultColor\":false,\"contentType\":\"TEXT_HTML\"},{\"type\":\"RGB\",\"id\":13,\"x\":2,\"y\":3,\"color\":616861439,\"width\":4,\"height\":3,\"tabId\":0,\"isDefaultColor\":false,\"deviceId\":0,\"splitMode\":false,\"sendOnReleaseOn\":true,\"frequency\":0,\"pins\":[{\"pin\":13,\"pwmMode\":false,\"rangeMappingOn\":false,\"pinType\":\"VIRTUAL\",\"value\":\"60\\u0000143\\u0000158\",\"min\":0.0,\"max\":255.0},{\"pin\":13,\"pwmMode\":false,\"rangeMappingOn\":false,\"pinType\":\"VIRTUAL\",\"value\":\"60\\u0000143\\u0000158\",\"min\":0.0,\"max\":255.0},{\"pin\":13,\"pwmMode\":false,\"rangeMappingOn\":false,\"pinType\":\"VIRTUAL\",\"value\":\"60\\u0000143\\u0000158\",\"min\":0.0,\"max\":255.0}]},{\"type\":\"TWO_AXIS_JOYSTICK\",\"id\":2,\"x\":2,\"y\":5,\"color\":600084223,\"width\":5,\"height\":4,\"tabId\":0,\"isDefaultColor\":false,\"deviceId\":0,\"split\":false,\"autoReturnOn\":true,\"portraitLocked\":false,\"frequency\":0,\"pins\":[{\"pin\":14,\"pwmMode\":false,\"rangeMappingOn\":false,\"pinType\":\"VIRTUAL\",\"value\":\"128\\u0000129\",\"min\":0.0,\"max\":255.0},{\"pin\":14,\"pwmMode\":false,\"rangeMappingOn\":false,\"pinType\":\"VIRTUAL\",\"value\":\"128\\u0000129\",\"min\":0.0,\"max\":255.0}]}],\"devices\":[{\"id\":0,\"boardType\":\"ESP8266\",\"connectTime\":0,\"isUserIcon\":false}],\"theme\":\"Blynk\",\"keepScreenOn\":false,\"isAppConnectedOn\":false,\"isNotificationsOff\":false,\"isShared\":false,\"isActive\":true,\"widgetBackgroundOn\":false}", consumeText(response));
        }
    }

}
