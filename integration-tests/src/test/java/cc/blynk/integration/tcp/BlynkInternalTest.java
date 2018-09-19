package cc.blynk.integration.tcp;

import cc.blynk.integration.SingleServerInstancePerTest;
import cc.blynk.integration.model.tcp.TestAppClient;
import cc.blynk.integration.model.tcp.TestHardClient;
import cc.blynk.server.core.model.Profile;
import cc.blynk.server.core.model.device.HardwareInfo;
import cc.blynk.server.core.model.serialization.JsonParser;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import static cc.blynk.integration.TestUtil.b;
import static cc.blynk.integration.TestUtil.hardware;
import static cc.blynk.integration.TestUtil.internal;
import static cc.blynk.integration.TestUtil.ok;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 2/2/2015.
 *
 */
@RunWith(MockitoJUnitRunner.class)
public class BlynkInternalTest extends SingleServerInstancePerTest {

    @Test
    public void testGetRTC() throws Exception {
        clientPair.appClient.createWidget(1, "{\"type\":\"RTC\",\"id\":99, \"pin\":99, \"pinType\":\"VIRTUAL\", " +
                "\"x\":0,\"y\":0,\"width\":0,\"height\":0}");

        clientPair.hardwareClient.send("internal rtc");
        String rtcResponse = clientPair.hardwareClient.getBody();
        assertNotNull(rtcResponse);

        String rtcTime = rtcResponse.split("\0")[1];

        assertNotNull(rtcTime);
        assertEquals(10, rtcTime.length());
        assertEquals(System.currentTimeMillis(), Long.parseLong(rtcTime) * 1000, 10000L);
    }

    @Test
    public void testHardwareLoginWithInfo() throws Exception {
        TestHardClient hardClient2 = new TestHardClient("localhost", properties.getHttpPort());
        hardClient2.start();

        hardClient2.login(clientPair.token);
        hardClient2.verifyResult(ok(1));

        hardClient2.send("internal " + b("ver 0.3.1 fw 3.3.3 h-beat 10 buff-in 256 dev Arduino cpu ATmega328P con W5100 tmpl tmpl00123"));

        hardClient2.verifyResult(ok(2));

        clientPair.appClient.reset();

        HardwareInfo hardwareInfo = new HardwareInfo("3.3.3", "0.3.1", "Arduino", "ATmega328P", "W5100", null, "tmpl00123", 10, 256);

        clientPair.appClient.send("loadProfileGzipped");
        Profile profile = clientPair.appClient.parseProfile(1);

        assertEquals(JsonParser.toJson(hardwareInfo), JsonParser.toJson(profile.dashBoards[0].devices[0].hardwareInfo));


        hardClient2.stop().awaitUninterruptibly();
    }

    @Test
    public void appConnectedEvent() throws Exception {
        clientPair.appClient.updateDash("{\"id\":1, \"name\":\"test board\", \"isAppConnectedOn\":true}");
        clientPair.appClient.verifyResult(ok(1));

        TestAppClient appClient = new TestAppClient(properties);
        appClient.start();

        appClient.login(getUserName(), "1", "Android", "1.13.3");
        appClient.verifyResult(ok(1));

        clientPair.hardwareClient.verifyResult(internal(7777, "acon"));
    }

    @Test
    public void appDisconnectedEvent() throws Exception {
        clientPair.appClient.updateDash("{\"id\":1, \"name\":\"test board\", \"isAppConnectedOn\":true}");
        clientPair.appClient.verifyResult(ok(1));

        clientPair.appClient.stop().await();

        clientPair.hardwareClient.verifyResult(internal(7777, "adis"));
    }

    @Test
    public void testBuffInIsHandled() throws Exception {
        clientPair.hardwareClient.send("internal " + b("ver 0.3.1 h-beat 10 buff-in 12 dev Arduino cpu ATmega328P con W5100 tmpl tmpl00123"));
        clientPair.hardwareClient.verifyResult(ok(1));

        clientPair.appClient.send("hardware 1-0 vw 1 12");
        clientPair.hardwareClient.verifyResult(hardware(1, "vw 1 12"));

        clientPair.appClient.send("hardware 1-0 vw 1 123");
        clientPair.hardwareClient.never(hardware(2, "vw 1 123"));

        clientPair.hardwareClient.send("internal " + b("ver 0.3.1 h-beat 10 dev Arduino cpu ATmega328P con W5100 tmpl tmpl00123"));
        clientPair.hardwareClient.verifyResult(ok(2));

        clientPair.appClient.send("hardware 1-0 vw 1 12");
        clientPair.hardwareClient.verifyResult(hardware(3, "vw 1 12"));

        clientPair.hardwareClient.send("internal " + b("ver 0.3.1 h-beat 10 buff-in 0 dev Arduino cpu ATmega328P con W5100 tmpl tmpl00123"));
        clientPair.hardwareClient.verifyResult(ok(3));

        clientPair.appClient.send("hardware 1-0 vw 1 12");
        clientPair.hardwareClient.verifyResult(hardware(4, "vw 1 12"));

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 245; i++) {
            sb.append("a");
        }

        String s = sb.toString();

        clientPair.appClient.send("hardware 1-0 vw 1 " + s);
        clientPair.hardwareClient.never(hardware(5, "vw 1 " + s));
    }

}
