package cc.blynk.integration.tcp;

import cc.blynk.integration.SingleServerInstancePerTest;
import cc.blynk.integration.TestUtil;
import cc.blynk.integration.model.tcp.ClientPair;
import cc.blynk.integration.model.tcp.TestAppClient;
import cc.blynk.integration.model.tcp.TestHardClient;
import cc.blynk.server.core.model.DataStream;
import cc.blynk.server.core.model.Profile;
import cc.blynk.server.core.model.device.Device;
import cc.blynk.server.core.model.widgets.MultiPinWidget;
import cc.blynk.server.core.model.widgets.OnePinWidget;
import cc.blynk.server.core.model.widgets.Widget;
import io.netty.channel.ChannelFuture;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import static cc.blynk.integration.TestUtil.hardwareConnected;
import static cc.blynk.integration.TestUtil.ok;
import static cc.blynk.integration.TestUtil.parseProfile;
import static cc.blynk.integration.TestUtil.readTestUserProfile;
import static cc.blynk.integration.TestUtil.saveProfile;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 28.04.16.
 */
@RunWith(MockitoJUnitRunner.class)
@Ignore("ignored cause requires token to work properly")
public class FacebookLoginTest extends SingleServerInstancePerTest {

    private final String facebookAuthToken = "";

    @Test
    public void testLoginWorksForNewUser() throws Exception {
        String host = "localhost";
        String email = "dima@gmail.com";

        ClientPair clientPair = TestUtil.initAppAndHardPair(host, properties.getHttpsPort(), properties.getHttpPort(), email, "1", "user_profile_json.txt", properties, 10000);

        ChannelFuture channelFuture = clientPair.appClient.stop();
        channelFuture.await();

        TestAppClient appClient = new TestAppClient(properties);
        appClient.start();
        appClient.send("login " + email + "\0" + facebookAuthToken + "\0" + "Android" + "\0" + "1.10.4" + "\0" + "facebook");
        verify(appClient.responseMock, timeout(1000)).channelRead(any(), eq(ok(1)));

        String expected = readTestUserProfile();

        appClient.reset();
        appClient.send("loadProfileGzipped");
        verify(appClient.responseMock, timeout(500)).channelRead(any(), any());

        Profile profile = appClient.parseProfile(1);
        profile.dashBoards[0].updatedAt = 0;
        assertEquals(expected, profile.toString());
    }

    @Test
    public void testFacebookLoginWorksForExistingUser() throws Exception {
        initFacebookAppAndHardPair("localhost", properties.getHttpsPort(), properties.getHttpPort(), "dima@gmail.com", facebookAuthToken);
    }

    private ClientPair initFacebookAppAndHardPair(String host, int appPort, int hardPort, String user, String facebookAuthToken) throws Exception {
        TestAppClient appClient = new TestAppClient(host, appPort, properties);
        TestHardClient hardClient = new TestHardClient(host, hardPort);

        appClient.start();
        hardClient.start();

        String userProfileString = readTestUserProfile(null);
        Profile profile = parseProfile(userProfileString);

        int expectedSyncCommandsCount = 0;
        for (Widget widget : profile.dashBoards[0].widgets) {
            if (widget instanceof OnePinWidget) {
                if (((OnePinWidget) widget).makeHardwareBody() != null) {
                    expectedSyncCommandsCount++;
                }
            } else if (widget instanceof MultiPinWidget) {
                MultiPinWidget multiPinWidget = ((MultiPinWidget) widget);
                if (multiPinWidget.dataStreams != null) {
                    for (DataStream dataStream : multiPinWidget.dataStreams) {
                        if (dataStream.notEmptyAndIsValid()) {
                            expectedSyncCommandsCount++;
                        }
                    }
                }
            }
        }

        int dashId = profile.dashBoards[0].id;

        appClient.send("login " + user + "\0" + facebookAuthToken + "\0" + "Android" + "\0" + "1.10.4" + "\0" + "facebook");
        verify(appClient.responseMock, timeout(1000)).channelRead(any(), eq(ok(1)));
        appClient.send("addEnergy " + 10000 + "\0" + "123456");
        verify(appClient.responseMock, timeout(1000)).channelRead(any(), eq(ok(2)));

        saveProfile(appClient, profile.dashBoards);

        appClient.activate(dashId);
        appClient.getDevice(dashId, 0);
        Device device = appClient.parseDevice(4 + profile.dashBoards.length + expectedSyncCommandsCount);
        String token = device.token;

        hardClient.login(token);
        verify(hardClient.responseMock, timeout(2000)).channelRead(any(), eq(ok(1)));
        verify(appClient.responseMock, timeout(2000)).channelRead(any(), eq(hardwareConnected(1, String.valueOf(dashId))));

        appClient.reset();
        hardClient.reset();

        return new ClientPair(appClient, hardClient, token);
    }


}
