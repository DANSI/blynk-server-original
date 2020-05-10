package cc.blynk.integration.tcp;

import cc.blynk.integration.SingleServerInstancePerTest;
import cc.blynk.integration.model.tcp.TestAppClient;
import cc.blynk.server.core.model.device.BoardType;
import cc.blynk.server.core.model.device.Device;
import cc.blynk.server.core.protocol.model.messages.ResponseMessage;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import static cc.blynk.integration.TestUtil.createDevice;
import static cc.blynk.integration.TestUtil.illegalCommand;
import static cc.blynk.integration.TestUtil.notAllowed;
import static cc.blynk.integration.TestUtil.ok;
import static cc.blynk.server.core.protocol.enums.Response.QUOTA_LIMIT;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.after;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.startsWith;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 2/2/2015.
 *
 */
@RunWith(MockitoJUnitRunner.class)
public class AppMailTest extends SingleServerInstancePerTest {

    @Test
    public void testSendEmail() throws Exception {
        TestAppClient appClient = new TestAppClient(properties);
        appClient.start();
        appClient.login(getUserName(), "1");
        appClient.verifyResult(ok(1));

        appClient.send("email 1");
        verify(holder.mailWrapper, timeout(1000)).sendText(eq(getUserName()), eq("Auth Token for My Dashboard project and device My Device"), startsWith("Auth Token : "));
    }

    @Test
    public void testSendEmailForDevice() throws Exception {
        TestAppClient appClient = new TestAppClient(properties);
        appClient.start();
        appClient.login(getUserName(), "1");
        appClient.verifyResult(ok(1));

        appClient.send("email 1 0");
        verify(holder.mailWrapper, timeout(1000)).sendText(eq(getUserName()), eq("Auth Token for My Dashboard project and device My Device"), startsWith("Auth Token : "));
    }

    @Test
    public void testSendEmailForSingleDevice() throws Exception {
        TestAppClient appClient = new TestAppClient(properties);
        appClient.start();
        appClient.login(getUserName(), "1");
        appClient.verifyResult(ok(1));

        clientPair.appClient.send("getDevices 1");
        Device[] devices = clientPair.appClient.parseDevices();

        assertEquals(1, devices.length);

        appClient.send("email 1");

        String expectedBody = String.format("Auth Token : %s\n" +
                "\n" +
                "Happy Blynking!\n" +
                "-\n" +
                "Getting Started Guide -> https://www.blynk.cc/getting-started\n" +
                "Documentation -> http://docs.blynk.cc/\n" +
                "Sketch generator -> https://examples.blynk.cc/\n" +
                "\n" +
                "Latest Blynk library -> https://github.com/blynkkk/blynk-library/releases/download/v0.6.1/Blynk_Release_v0.6.1.zip\n" +
                "Latest Blynk server -> https://github.com/blynkkk/blynk-server/releases/download/v0.41.13/server-0.41.13.jar\n" +
                "-\n" +
                "https://www.blynk.cc\n" +
                "twitter.com/blynk_app\n" +
                "www.facebook.com/blynkapp\n", devices[0].token);

        verify(holder.mailWrapper, timeout(1000)).sendText(eq(getUserName()), eq("Auth Token for My Dashboard project and device My Device"), eq(expectedBody));
    }

    @Test
    public void testSendEmailForMultiDevices() throws Exception {
        TestAppClient appClient = new TestAppClient(properties);
        appClient.start();
        appClient.login(getUserName(), "1");
        appClient.verifyResult(ok(1));

        Device device1 = new Device(1, "My Device2", BoardType.ESP8266);

        clientPair.appClient.createDevice(1, device1);
        Device device = clientPair.appClient.parseDevice();

        assertNotNull(device);
        assertNotNull(device.token);
        clientPair.appClient.verifyResult(createDevice(1, device));

        clientPair.appClient.send("getDevices 1");
        Device[] devices = clientPair.appClient.parseDevices(2);

        appClient.send("email 1");

        String expectedBody = String.format("Auth Token for device 'My Device' : %s\n" +
                "Auth Token for device 'My Device2' : %s\n" +
                "\n" +
                "Happy Blynking!\n" +
                "-\n" +
                "Getting Started Guide -> https://www.blynk.cc/getting-started\n" +
                "Documentation -> http://docs.blynk.cc/\n" +
                "Sketch generator -> https://examples.blynk.cc/\n" +
                "\n" +
                "Latest Blynk library -> https://github.com/blynkkk/blynk-library/releases/download/v0.6.1/Blynk_Release_v0.6.1.zip\n" +
                "Latest Blynk server -> https://github.com/blynkkk/blynk-server/releases/download/v0.41.13/server-0.41.13.jar\n" +
                "-\n" +
                "https://www.blynk.cc\n" +
                "twitter.com/blynk_app\n" +
                "www.facebook.com/blynkapp\n", devices[0].token, devices[1].token);

        verify(holder.mailWrapper, timeout(1000)).sendText(eq(getUserName()), eq("Auth Tokens for My Dashboard project and 2 devices"), eq(expectedBody));
    }

    @Test
    public void testEmailMininalValidation() throws Exception {
        //adding email widget
        clientPair.appClient.createWidget(1, "{\"id\":432, \"width\":1, \"height\":1, \"x\":0, \"y\":0, \"type\":\"EMAIL\"}");
        clientPair.appClient.verifyResult(ok(1));

        clientPair.hardwareClient.send("email to subj body");
        verify(holder.mailWrapper, after(500).never()).sendHtml(eq("to"), eq("subj"), eq("body"));
        clientPair.hardwareClient.verifyResult(illegalCommand(1));
    }

    @Test
    public void testEmailWorks() throws Exception {
        //no email widget
        clientPair.hardwareClient.send("email to subj body");
        clientPair.hardwareClient.verifyResult(notAllowed(1));

        //adding email widget
        clientPair.appClient.createWidget(1, "{\"id\":432, \"width\":1, \"height\":1, \"x\":0, \"y\":0, \"type\":\"EMAIL\"}");
        clientPair.appClient.verifyResult(ok(1));

        clientPair.hardwareClient.send("email to@to.com subj body");
        verify(holder.mailWrapper, timeout(500)).sendHtml(eq("to@to.com"), eq("subj"), eq("body"));
        clientPair.hardwareClient.verifyResult(ok(2));

        clientPair.hardwareClient.send("email to@to.com subj body");
        clientPair.hardwareClient.verifyResult(new ResponseMessage(3, QUOTA_LIMIT));
    }

    @Test
    public void testPlainTextIsAllowed() throws Exception {
        //adding email widget
        clientPair.appClient.createWidget(1, "{\"id\":432, \"contentType\":\"TEXT_PLAIN\", \"width\":1, \"height\":1, \"x\":0, \"y\":0, \"type\":\"EMAIL\"}");
        clientPair.appClient.verifyResult(ok(1));

        clientPair.hardwareClient.send("email to@to.com subj body");
        verify(holder.mailWrapper, timeout(500)).sendText(eq("to@to.com"), eq("subj"), eq("body"));
        clientPair.hardwareClient.verifyResult(ok(1));
    }

    @Test
    public void testPlaceholderForDeviceNameWorks() throws Exception {
        //adding email widget
        clientPair.appClient.createWidget(1, "{\"id\":432, \"contentType\":\"TEXT_PLAIN\", \"width\":1, \"height\":1, \"x\":0, \"y\":0, \"type\":\"EMAIL\"}");
        clientPair.appClient.verifyResult(ok(1));

        clientPair.hardwareClient.send("email to@to.com SUBJ_{DEVICE_NAME} BODY_{DEVICE_NAME}");
        verify(holder.mailWrapper, timeout(500)).sendText(eq("to@to.com"), eq("SUBJ_My Device"), eq("BODY_My Device"));
        clientPair.hardwareClient.verifyResult(ok(1));
    }

    @Test
    public void testPlaceholderForVendorWorks() throws Exception {
        //adding email widget
        clientPair.appClient.createWidget(1, "{\"id\":432, \"contentType\":\"TEXT_PLAIN\", \"width\":1, \"height\":1, \"x\":0, \"y\":0, \"type\":\"EMAIL\"}");
        clientPair.appClient.verifyResult(ok(1));

        clientPair.hardwareClient.send("email {VENDOR_EMAIL} SUBJ_{VENDOR_EMAIL} BODY_{VENDOR_EMAIL}");
        verify(holder.mailWrapper, timeout(500)).sendText(eq("vendor@blynk.cc"), eq("SUBJ_vendor@blynk.cc"), eq("BODY_vendor@blynk.cc"));
        clientPair.hardwareClient.verifyResult(ok(1));
    }

    @Test
    public void testPlaceholderForDeviceOwnerWorks() throws Exception {
        //adding email widget
        clientPair.appClient.createWidget(1, "{\"id\":432, \"contentType\":\"TEXT_PLAIN\", \"width\":1, \"height\":1, \"x\":0, \"y\":0, \"type\":\"EMAIL\"}");
        clientPair.appClient.verifyResult(ok(1));

        clientPair.hardwareClient.send("email {DEVICE_OWNER_EMAIL} SUBJ_{DEVICE_OWNER_EMAIL} BODY_{DEVICE_OWNER_EMAIL}");
        verify(holder.mailWrapper, timeout(500)).sendText(eq(getUserName()), eq("SUBJ_" + getUserName()), eq("BODY_" + getUserName()));
        clientPair.hardwareClient.verifyResult(ok(1));
    }

    @Test
    public void testEmailWorkWithEmailFromApp() throws Exception {
        //adding email widget
        clientPair.appClient.createWidget(1, "{\"id\":432, \"width\":1, \"height\":1, \"x\":0, \"y\":0, \"to\":\"test@mail.ua\", \"type\":\"EMAIL\"}");
        clientPair.appClient.verifyResult(ok(1));

        clientPair.hardwareClient.send("email subj body");
        verify(holder.mailWrapper, timeout(500)).sendHtml(eq("test@mail.ua"), eq("subj"), eq("body"));
        clientPair.hardwareClient.verifyResult(ok(1));
    }

    @Test
    public void testEmailFromAppOverridesEmailFromHardware() throws Exception {
        //adding email widget
        clientPair.appClient.createWidget(1, "{\"id\":432, \"width\":1, \"height\":1, \"x\":0, \"y\":0, \"to\":\"test@mail.ua\", \"type\":\"EMAIL\"}");
        clientPair.appClient.verifyResult(ok(1));

        clientPair.hardwareClient.send("email to@to.com subj body");
        verify(holder.mailWrapper, timeout(500)).sendHtml(eq("test@mail.ua"), eq("subj"), eq("body"));
        clientPair.hardwareClient.verifyResult(ok(1));
    }

    @Test
    public void testEmailWorkWithNoEmailInApp() throws Exception {
        //adding email widget
        clientPair.appClient.createWidget(1, "{\"id\":432, \"width\":1, \"height\":1, \"x\":0, \"y\":0, \"width\":1, \"height\":1, \"type\":\"EMAIL\"}");
        clientPair.appClient.verifyResult(ok(1));

        clientPair.hardwareClient.send("email subj body");
        verify(holder.mailWrapper, timeout(500)).sendHtml(eq(getUserName()), eq("subj"), eq("body"));
        clientPair.hardwareClient.verifyResult(ok(1));
    }

}
