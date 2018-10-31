package cc.blynk.integration.http;

import cc.blynk.integration.SingleServerInstancePerTest;
import cc.blynk.integration.TestUtil;
import cc.blynk.integration.model.tcp.TestHardClient;
import cc.blynk.integration.tcp.EventorTest;
import cc.blynk.server.core.model.DashBoard;
import cc.blynk.server.core.model.DataStream;
import cc.blynk.server.core.model.device.BoardType;
import cc.blynk.server.core.model.device.Device;
import cc.blynk.server.core.model.enums.PinType;
import cc.blynk.server.core.model.widgets.controls.RGB;
import cc.blynk.server.core.model.widgets.controls.Timer;
import cc.blynk.server.core.model.widgets.others.eventor.Eventor;
import cc.blynk.server.core.model.widgets.others.eventor.Rule;
import cc.blynk.server.core.model.widgets.others.eventor.TimerTime;
import cc.blynk.server.core.model.widgets.others.eventor.model.action.BaseAction;
import cc.blynk.server.core.model.widgets.others.eventor.model.action.SetPinAction;
import cc.blynk.server.core.model.widgets.others.eventor.model.action.SetPinActionType;
import cc.blynk.server.core.model.widgets.others.rtc.RTC;
import cc.blynk.server.core.model.widgets.others.webhook.WebHook;
import cc.blynk.server.core.model.widgets.outputs.ValueDisplay;
import cc.blynk.server.core.model.widgets.outputs.graph.GraphDataStream;
import cc.blynk.server.core.model.widgets.outputs.graph.GraphType;
import cc.blynk.server.core.model.widgets.outputs.graph.Superchart;
import cc.blynk.server.core.model.widgets.ui.table.Table;
import cc.blynk.server.servers.application.MobileAndHttpsServer;
import cc.blynk.server.servers.hardware.HardwareAndHttpAPIServer;
import cc.blynk.utils.DateTimeUtils;
import cc.blynk.utils.properties.ServerProperties;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.LuminanceSource;
import com.google.zxing.Result;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.QRCodeReader;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static cc.blynk.integration.BaseTest.getRelativeDataFolder;
import static cc.blynk.integration.TestUtil.b;
import static cc.blynk.integration.TestUtil.consumeText;
import static cc.blynk.integration.TestUtil.createDefaultHolder;
import static cc.blynk.integration.TestUtil.createDevice;
import static cc.blynk.integration.TestUtil.hardware;
import static cc.blynk.integration.TestUtil.ok;
import static cc.blynk.server.core.model.enums.PinType.VIRTUAL;
import static cc.blynk.server.core.protocol.enums.Command.HARDWARE;
import static cc.blynk.server.core.protocol.model.messages.MessageFactory.produce;
import static cc.blynk.server.workers.timer.TimerWorker.TIMER_MSG_ID;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.after;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 07.01.16.
 */
@RunWith(MockitoJUnitRunner.class)
public class HttpAndTCPSameJVMTest extends SingleServerInstancePerTest {

    private static CloseableHttpClient httpclient;
    private static String httpServerUrl;

    @AfterClass
    public static void closeHttp() throws Exception {
        httpclient.close();
    }

    @BeforeClass
    //shadow parent method by purpose
    public static void init() throws Exception {
        properties = new ServerProperties(Collections.emptyMap());
        properties.setProperty("data.folder", getRelativeDataFolder("/profiles"));
        holder = createDefaultHolder(properties, "no-db.properties");
        hardwareServer = new HardwareAndHttpAPIServer(holder).start();
        appServer = new MobileAndHttpsServer(holder).start();

        httpServerUrl = String.format("http://localhost:%s/", properties.getHttpPort());
        httpclient = HttpClients.createDefault();
    }

    @Test
    public void testChangeNonWidgetPinValueViaHardwareAndGetViaHTTP() throws Exception {
        clientPair.hardwareClient.send("hardware vw 10 200");
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(produce(1, HARDWARE, b("1-0 vw 10 200"))));

        reset(clientPair.appClient.responseMock);

        HttpGet request = new HttpGet(httpServerUrl + clientPair.token + "/get/v10");

        try (CloseableHttpResponse response = httpclient.execute(request)) {
            assertEquals(200, response.getStatusLine().getStatusCode());
            List<String> values = TestUtil.consumeJsonPinValues(response);
            assertEquals(1, values.size());
            assertEquals("200", values.get(0));
        }

        clientPair.appClient.send("hardware 1 vw 10 201");
        verify(clientPair.hardwareClient.responseMock, timeout(500)).channelRead(any(), eq(produce(1, HARDWARE, b("vw 10 201"))));

        try (CloseableHttpResponse response = httpclient.execute(request)) {
            assertEquals(200, response.getStatusLine().getStatusCode());
            List<String> values = TestUtil.consumeJsonPinValues(response);
            assertEquals(1, values.size());
            assertEquals("201", values.get(0));
        }
    }

    @Test
    public void testChangePinValueViaAppAndHardware() throws Exception {
        clientPair.hardwareClient.send("hardware vw 4 200");
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(produce(1, HARDWARE, b("1-0 vw 4 200"))));

        reset(clientPair.appClient.responseMock);

        HttpGet request = new HttpGet(httpServerUrl + clientPair.token + "/get/v4");

        try (CloseableHttpResponse response = httpclient.execute(request)) {
            assertEquals(200, response.getStatusLine().getStatusCode());
            List<String> values = TestUtil.consumeJsonPinValues(response);
            assertEquals(1, values.size());
            assertEquals("200", values.get(0));
        }

        clientPair.appClient.send("hardware 1 vw 4 201");
        verify(clientPair.hardwareClient.responseMock, timeout(500)).channelRead(any(), eq(produce(1, HARDWARE, b("vw 4 201"))));

        try (CloseableHttpResponse response = httpclient.execute(request)) {
            assertEquals(200, response.getStatusLine().getStatusCode());
            List<String> values = TestUtil.consumeJsonPinValues(response);
            assertEquals(1, values.size());
            assertEquals("201", values.get(0));
        }
    }

    @Test
    public void testRTCWorksViaHttpAPI() throws Exception {
        RTC rtc = new RTC();
        rtc.id = 434;
        rtc.height = 1;
        rtc.width = 2;

        clientPair.appClient.createWidget(1, rtc);
        clientPair.appClient.verifyResult(ok(1));

        reset(clientPair.appClient.responseMock);

        HttpGet request = new HttpGet(httpServerUrl + clientPair.token + "/rtc");

        try (CloseableHttpResponse response = httpclient.execute(request)) {
            assertEquals(200, response.getStatusLine().getStatusCode());
            List<String> values = TestUtil.consumeJsonPinValues(response);
            assertEquals(1, values.size());
        }
    }

    @Test
    public void testEventorWorksViaHttpAPI() throws Exception {
        Eventor eventor = EventorTest.oneRuleEventor("if v100 = 37 then setpin v2 123");
        eventor.height = 1;
        eventor.width = 2;

        clientPair.appClient.createWidget(1, eventor);
        clientPair.appClient.verifyResult(ok(1));

        reset(clientPair.appClient.responseMock);

        HttpPut request = new HttpPut(httpServerUrl + clientPair.token + "/update/v100");
        request.setEntity(new StringEntity("[\"37\"]", ContentType.APPLICATION_JSON));

        try (CloseableHttpResponse response = httpclient.execute(request)) {
            assertEquals(200, response.getStatusLine().getStatusCode());
        }

        clientPair.appClient.verifyResult(hardware(111, "1-0 vw 100 37"));
        clientPair.hardwareClient.verifyResult(hardware(111, "vw 100 37"));
        clientPair.hardwareClient.verifyResult(hardware(888, "vw 2 123"));
    }

    @Test
    public void testEventorTimerWidgeWorkerWorksAsExpectedWithHttp() throws Exception {
        Executors.newScheduledThreadPool(1).scheduleAtFixedRate(holder.timerWorker, 0, 1000, TimeUnit.MILLISECONDS);

        TimerTime timerTime = new TimerTime(
                0,
                new int[] {1,2,3,4,5,6,7},
                //adding 2 seconds just to be sure we no gonna miss timer event
                LocalTime.now(DateTimeUtils.UTC).toSecondOfDay() + 2,
                DateTimeUtils.UTC
        );

        DataStream dataStream = new DataStream((short) 4, VIRTUAL);
        SetPinAction setPinAction = new SetPinAction(dataStream, "1", SetPinActionType.CUSTOM);

        Eventor eventor = new Eventor(new Rule[] {
                new Rule(dataStream, timerTime, null, new BaseAction[] {setPinAction}, true)
        });
        eventor.id = 1000;

        clientPair.appClient.createWidget(1, eventor);
        clientPair.appClient.verifyResult(ok(1));

        verify(clientPair.appClient.responseMock, timeout(3000)).channelRead(any(), eq(produce(TIMER_MSG_ID, HARDWARE, b("1-0 vw 4 1"))));
        verify(clientPair.hardwareClient.responseMock, timeout(3000)).channelRead(any(), eq(produce(TIMER_MSG_ID, HARDWARE, b("vw 4 1"))));



        clientPair.appClient.reset();
        HttpGet requestGET = new HttpGet(httpServerUrl + clientPair.token + "/get/v4");

        try (CloseableHttpResponse response = httpclient.execute(requestGET)) {
            assertEquals(200, response.getStatusLine().getStatusCode());
            List<String> values = TestUtil.consumeJsonPinValues(response);
            assertEquals(1, values.size());
            assertEquals("1", values.get(0));
        }
    }

    @Test
    public void testTimerWidgeWorkerWorksAsExpectedWithHttp() throws Exception {
        Executors.newScheduledThreadPool(1).scheduleAtFixedRate(holder.timerWorker, 0, 1000, TimeUnit.MILLISECONDS);
        Timer timer = new Timer();
        timer.id = 112;
        timer.x = 1;
        timer.y = 1;
        timer.pinType = VIRTUAL;
        timer.pin = 4;
        timer.width = 2;
        timer.height = 1;
        timer.startValue = "1";
        timer.stopValue = "0";
        LocalTime localDateTime = LocalTime.now(ZoneId.of("UTC"));
        int curTime = localDateTime.toSecondOfDay();
        timer.startTime = curTime + 1;
        timer.stopTime = curTime + 1;

        clientPair.appClient.createWidget(1, timer);
        clientPair.appClient.verifyResult(ok(1));

        verify(clientPair.hardwareClient.responseMock, timeout(2500).times(2)).channelRead(any(), any());
        verify(clientPair.hardwareClient.responseMock, timeout(500)).channelRead(any(), eq(produce(7777, HARDWARE, b("vw 4 1"))));
        verify(clientPair.hardwareClient.responseMock, timeout(500)).channelRead(any(), eq(produce(7777, HARDWARE, b("vw 4 0"))));

        verify(clientPair.appClient.responseMock, timeout(2500).times(3)).channelRead(any(), any());
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(produce(7777, HARDWARE, b("1-0 vw 4 1"))));
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(produce(7777, HARDWARE, b("1-0 vw 4 0"))));

        clientPair.appClient.reset();
        HttpGet requestGET = new HttpGet(httpServerUrl + clientPair.token + "/get/v4");

        try (CloseableHttpResponse response = httpclient.execute(requestGET)) {
            assertEquals(200, response.getStatusLine().getStatusCode());
            List<String> values = TestUtil.consumeJsonPinValues(response);
            assertEquals(1, values.size());
            //todo order is not guarateed here!!! Known issue
            String res = values.get(0);
            assertTrue("0".equals(res) || "1".equals(res));
        }
    }

    @Test
    public void testChangePinValueViaAppAndHardwareForWrongPWMButton() throws Exception {
        clientPair.appClient.createWidget(1, "{\"type\":\"BUTTON\",\"id\":1000,\"x\":0,\"y\":0,\"color\":616861439,\"width\":2,\"height\":2,\"label\":\"Relay\",\"pinType\":\"DIGITAL\",\"pin\":18,\"pwmMode\":true,\"rangeMappingOn\":false,\"min\":0,\"max\":0,\"value\":\"1\",\"pushMode\":false}");
        clientPair.appClient.verifyResult(ok(1));

        clientPair.appClient.reset();
        HttpGet requestGET = new HttpGet(httpServerUrl + clientPair.token + "/get/d18");

        try (CloseableHttpResponse response = httpclient.execute(requestGET)) {
            assertEquals(200, response.getStatusLine().getStatusCode());
            List<String> values = TestUtil.consumeJsonPinValues(response);
            assertEquals(1, values.size());
            assertEquals("1", values.get(0));
        }

        HttpPut requestPUT = new HttpPut(httpServerUrl + clientPair.token + "/update/d18");
        requestPUT.setEntity(new StringEntity("[\"0\"]", ContentType.APPLICATION_JSON));

        try (CloseableHttpResponse response = httpclient.execute(requestPUT)) {
            assertEquals(200, response.getStatusLine().getStatusCode());
        }

        verify(clientPair.hardwareClient.responseMock, timeout(500)).channelRead(any(), eq(produce(111, HARDWARE, b("dw 18 0"))));
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(produce(111, HARDWARE, b("1-0 dw 18 0"))));
    }

    @Test
    public void testChangePinValueViaHttpAPI() throws Exception {
        HttpPut request = new HttpPut(httpServerUrl + clientPair.token + "/update/v4");
        HttpGet getRequest = new HttpGet(httpServerUrl + clientPair.token + "/get/v4");

        for (int i = 0; i < 50; i++) {
            request.setEntity(new StringEntity("[\"" + i + "\"]", ContentType.APPLICATION_JSON));
            try (CloseableHttpResponse response = httpclient.execute(request)) {
                assertEquals(200, response.getStatusLine().getStatusCode());
            }

            clientPair.hardwareClient.sync(VIRTUAL, 4);
            verify(clientPair.hardwareClient.responseMock, timeout(500)).channelRead(any(), eq(produce(i + 1, HARDWARE, b("vw 4 " + i))));

            try (CloseableHttpResponse response = httpclient.execute(getRequest)) {
                assertEquals(200, response.getStatusLine().getStatusCode());
                List<String> values = TestUtil.consumeJsonPinValues(response);
                assertEquals(1, values.size());
                assertEquals(i, Integer.valueOf(values.get(0)).intValue());
            }
        }
    }

    @Test
    public void testQRWorks() throws Exception {
        HttpGet getRequest = new HttpGet(httpServerUrl + clientPair.token + "/qr");
        String cloneToken;
        try (CloseableHttpResponse response = httpclient.execute(getRequest)) {
            assertEquals(200, response.getStatusLine().getStatusCode());
            assertEquals("image/png", response.getFirstHeader("Content-Type").getValue());
            byte[] data = EntityUtils.toByteArray(response.getEntity());
            assertNotNull(data);

            //get the data from the input stream
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(data));

            //convert the image to a binary bitmap source
            LuminanceSource source = new BufferedImageLuminanceSource(image);
            BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));
            QRCodeReader reader = new QRCodeReader();
            Result result = reader.decode(bitmap);
            String resultString = result.getText();
            assertTrue(resultString.startsWith("blynk://token/clone/"));
            assertTrue(resultString.endsWith("?server=127.0.0.1&port=10443"));
            cloneToken = resultString.substring(
                    resultString.indexOf("blynk://token/clone/") + "blynk://token/clone/".length(),
                    resultString.indexOf("?server=127.0.0.1&port=10443"));
            assertEquals(32, cloneToken.length());
        }

        clientPair.appClient.send("getProjectByCloneCode " + cloneToken);
        DashBoard dashBoard = clientPair.appClient.parseDash(1);
        assertEquals("My Dashboard", dashBoard.name);
    }

    @Test
    public void testIsHardwareAndAppConnected() throws Exception {
        HttpGet request = new HttpGet(httpServerUrl + clientPair.token + "/isHardwareConnected");

        try (CloseableHttpResponse response = httpclient.execute(request)) {
            assertEquals(200, response.getStatusLine().getStatusCode());
            String value = consumeText(response);
            assertNotNull(value);
            assertEquals("true", value);
        }

        request = new HttpGet(httpServerUrl + clientPair.token + "/isAppConnected");

        try (CloseableHttpResponse response = httpclient.execute(request)) {
            assertEquals(200, response.getStatusLine().getStatusCode());
            String value = consumeText(response);
            assertNotNull(value);
            assertEquals("true", value);
        }
    }

    @Test
    public void testIsHardwareAndAppDisconnected() throws Exception {
        clientPair.stop();

        HttpGet request = new HttpGet(httpServerUrl + clientPair.token + "/isHardwareConnected");

        try (CloseableHttpResponse response = httpclient.execute(request)) {
            assertEquals(200, response.getStatusLine().getStatusCode());
            String value = consumeText(response);
            assertNotNull(value);
            assertEquals("false", value);
        }

        request = new HttpGet(httpServerUrl + clientPair.token + "/isAppConnected");

        try (CloseableHttpResponse response = httpclient.execute(request)) {
            assertEquals(200, response.getStatusLine().getStatusCode());
            String value = consumeText(response);
            assertNotNull(value);
            assertEquals("false", value);
        }
    }

    @Test
    public void testIsHardwareConnecteedWithMultiDevices() throws Exception {
        HttpGet request = new HttpGet(httpServerUrl + clientPair.token + "/isHardwareConnected");

        try (CloseableHttpResponse response = httpclient.execute(request)) {
            assertEquals(200, response.getStatusLine().getStatusCode());
            String value = consumeText(response);
            assertNotNull(value);
            assertEquals("true", value);
        }

        request = new HttpGet(httpServerUrl + clientPair.token + "/isAppConnected");

        try (CloseableHttpResponse response = httpclient.execute(request)) {
            assertEquals(200, response.getStatusLine().getStatusCode());
            String value = consumeText(response);
            assertNotNull(value);
            assertEquals("true", value);
        }

        Device device1 = new Device(1, "My Device", BoardType.ESP8266);

        clientPair.appClient.createDevice(1, device1);
        Device device = clientPair.appClient.parseDevice(1);
        assertNotNull(device);
        assertNotNull(device.token);
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(createDevice(1, device)));

        clientPair.appClient.reset();

        clientPair.appClient.send("getDevices 1");
        Device[] devices = clientPair.appClient.parseDevices();

        assertNotNull(devices);
        assertEquals(2, devices.length);

        TestHardClient hardClient2 = new TestHardClient("localhost", properties.getHttpPort());
        hardClient2.start();

        hardClient2.login(devices[1].token);
        hardClient2.verifyResult(ok(1));

        clientPair.stop();

        request = new HttpGet(httpServerUrl + clientPair.token + "/isHardwareConnected");

        try (CloseableHttpResponse response = httpclient.execute(request)) {
            assertEquals(200, response.getStatusLine().getStatusCode());
            String value = consumeText(response);
            assertNotNull(value);
            assertEquals("false", value);
        }

        request = new HttpGet(httpServerUrl + clientPair.token + "/isAppConnected");

        try (CloseableHttpResponse response = httpclient.execute(request)) {
            assertEquals(200, response.getStatusLine().getStatusCode());
            String value = consumeText(response);
            assertNotNull(value);
            assertEquals("false", value);
        }

        request = new HttpGet(httpServerUrl + devices[1].token + "/isHardwareConnected");

        try (CloseableHttpResponse response = httpclient.execute(request)) {
            assertEquals(200, response.getStatusLine().getStatusCode());
            String value = consumeText(response);
            assertNotNull(value);
            assertEquals("true", value);
        }
    }

    @Test
    public void testChangePinValueViaHttpAPIAndNoActiveProject() throws Exception {
        clientPair.appClient.deactivate(1);
        clientPair.appClient.verifyResult(ok(1));

        HttpPut request = new HttpPut(httpServerUrl + clientPair.token + "/update/v31");

        request.setEntity(new StringEntity("[\"100\"]", ContentType.APPLICATION_JSON));
        try (CloseableHttpResponse response = httpclient.execute(request)) {
            assertEquals(200, response.getStatusLine().getStatusCode());
        }

        verify(clientPair.hardwareClient.responseMock, timeout(500)).channelRead(any(), eq(produce(111, HARDWARE, b("vw 31 100"))));
        verify(clientPair.appClient.responseMock, after(400).never()).channelRead(any(), eq(produce(111, HARDWARE, b("1 vw 31 100"))));

        clientPair.appClient.activate(1);
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(ok(2)));
    }

    @Test
    public void testChangeLCDPinValueViaHttpAPIAndValueChanged() throws Exception {
        HttpPut request = new HttpPut(httpServerUrl + clientPair.token + "/update/v0");

        request.setEntity(new StringEntity("[\"100\"]", ContentType.APPLICATION_JSON));
        try (CloseableHttpResponse response = httpclient.execute(request)) {
            assertEquals(200, response.getStatusLine().getStatusCode());
        }

        verify(clientPair.hardwareClient.responseMock, timeout(500)).channelRead(any(), eq(produce(111, HARDWARE, b("vw 0 100"))));
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(produce(111, HARDWARE, b("1-0 vw 0 100"))));

        request = new HttpPut(httpServerUrl + clientPair.token + "/update/v1");

        request.setEntity(new StringEntity("[\"101\"]", ContentType.APPLICATION_JSON));
        try (CloseableHttpResponse response = httpclient.execute(request)) {
            assertEquals(200, response.getStatusLine().getStatusCode());
        }

        verify(clientPair.hardwareClient.responseMock, timeout(500)).channelRead(any(), eq(produce(111, HARDWARE, b("vw 1 101"))));
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(produce(111, HARDWARE, b("1-0 vw 1 101"))));
    }

    @Test
    public void testChangePinValueViaHttpAPIAndNoWidgetSinglePinValue() throws Exception {
        HttpPut request = new HttpPut(httpServerUrl + clientPair.token + "/update/v31");

        request.setEntity(new StringEntity("[\"100\"]", ContentType.APPLICATION_JSON));
        try (CloseableHttpResponse response = httpclient.execute(request)) {
            assertEquals(200, response.getStatusLine().getStatusCode());
        }

        verify(clientPair.hardwareClient.responseMock, timeout(500)).channelRead(any(), eq(produce(111, HARDWARE, b("vw 31 100"))));
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(produce(111, HARDWARE, b("1-0 vw 31 100"))));
    }

    @Test
    public void testChangePinValueViaHttpAPIAndForTerminal() throws Exception {
        clientPair.appClient.createWidget(1, "{\"id\":222, \"width\":1, \"height\":1, \"x\":2, \"y\":2, \"label\":\"Some Text 2\", \"type\":\"TERMINAL\", \"pinType\":\"VIRTUAL\", \"pin\":100}");
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(ok(1)));

        HttpPut request = new HttpPut(httpServerUrl + clientPair.token + "/update/V100");

        request.setEntity(new StringEntity("[\"100\"]", ContentType.APPLICATION_JSON));
        try (CloseableHttpResponse response = httpclient.execute(request)) {
            assertEquals(200, response.getStatusLine().getStatusCode());
        }

        verify(clientPair.hardwareClient.responseMock, timeout(500)).channelRead(any(), eq(produce(111, HARDWARE, b("vw 100 100"))));
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(produce(111, HARDWARE, b("1-0 vw 100 100"))));
    }

    @Test
    public void testChangePinValueViaHttpAPIAndNoWidgetMultiPinValue() throws Exception {
        HttpPut request = new HttpPut(httpServerUrl + clientPair.token + "/update/v31");

        request.setEntity(new StringEntity("[\"100\",\"101\",\"102\"]", ContentType.APPLICATION_JSON));
        try (CloseableHttpResponse response = httpclient.execute(request)) {
            assertEquals(200, response.getStatusLine().getStatusCode());
        }

        verify(clientPair.hardwareClient.responseMock, timeout(500)).channelRead(any(), eq(produce(111, HARDWARE, b("vw 31 100 101 102"))));
    }

    @Test
    public void tableSetValueViaHttpApi() throws Exception {
        Table table = new Table();
        table.pin = 123;
        table.pinType = VIRTUAL;
        table.isClickableRows = true;
        table.isReoderingAllowed = true;
        table.height = 2;
        table.width = 2;

        clientPair.appClient.createWidget(1, table);
        clientPair.appClient.verifyResult(ok(1));

        HttpGet updateTableRow = new HttpGet(httpServerUrl + clientPair.token + "/update/v123?value=add&value=2&value=Martes&value=120Kwh");
        try (CloseableHttpResponse response = httpclient.execute(updateTableRow)) {
            assertEquals(200, response.getStatusLine().getStatusCode());
        }
    }

    @Test
    public void sendMultiValueToAppViaHttpApi() throws Exception {
        HttpGet updateTableRow = new HttpGet(httpServerUrl + clientPair.token + "/update/V1?value=110&value=230&value=330");
        try (CloseableHttpResponse response = httpclient.execute(updateTableRow)) {
            assertEquals(200, response.getStatusLine().getStatusCode());
        }

        verify(clientPair.hardwareClient.responseMock, timeout(500)).channelRead(any(),
                eq(produce(111, HARDWARE, b("vw 1 110 230 330"))));
    }

    @Test
    public void sendMultiValueToAppViaHttpApi2() throws Exception {
        RGB rgb = new RGB();
        rgb.dataStreams = new DataStream[] {
                new DataStream((short) 101, VIRTUAL)
        };
        rgb.splitMode = false;
        rgb.height = 2;
        rgb.width = 2;

        clientPair.appClient.createWidget(1, rgb);
        clientPair.appClient.verifyResult(ok(1));

        HttpGet updateTableRow = new HttpGet(httpServerUrl + clientPair.token + "/update/V101?value=110&value=230&value=330");
        try (CloseableHttpResponse response = httpclient.execute(updateTableRow)) {
            assertEquals(200, response.getStatusLine().getStatusCode());
        }

        verify(clientPair.hardwareClient.responseMock, timeout(500)).channelRead(any(),
                eq(produce(111, HARDWARE, b("vw 101 110 230 330"))));
    }

    @Test
    public void sendMultiValueToAppViaHttpApi3() throws Exception {
        RGB rgb = new RGB();
        rgb.dataStreams = new DataStream[] {
                new DataStream((short) 101, VIRTUAL),
                new DataStream((short) 102, VIRTUAL),
                new DataStream((short) 103, VIRTUAL)
        };
        rgb.splitMode = false;
        rgb.height = 2;
        rgb.width = 2;

        clientPair.appClient.createWidget(1, rgb);
        clientPair.appClient.verifyResult(ok(1));

        HttpGet updateTableRow = new HttpGet(httpServerUrl + clientPair.token + "/update/V101?value=110&value=230&value=330");
        try (CloseableHttpResponse response = httpclient.execute(updateTableRow)) {
            assertEquals(200, response.getStatusLine().getStatusCode());
        }

        verify(clientPair.hardwareClient.responseMock, timeout(500)).channelRead(any(),
                eq(produce(111, HARDWARE, b("vw 101 110 230 330"))));
    }

    @Test
    public void superchartPinsOverlapsWithOtherWidgets() throws Exception {
        Superchart superchart = new Superchart();
        superchart.id = 100;
        superchart.width = 8;
        superchart.height = 4;
        DataStream dataStream = new DataStream((short) 44, PinType.VIRTUAL);
        DataStream dataStream2 = new DataStream((short) 45, PinType.VIRTUAL);
        GraphDataStream graphDataStream = new GraphDataStream(null, GraphType.LINE, 0, 0, dataStream, null, 0, null, null, null, 0, 0, false, null, false, false, false, null, 0, false, 0);
        GraphDataStream graphDataStream2 = new GraphDataStream(null, GraphType.LINE, 0, 0, dataStream2, null, 0, null, null, null, 0, 0, false, null, false, false, false, null, 0, false, 0);
        superchart.dataStreams = new GraphDataStream[] {
                graphDataStream,
                graphDataStream2
        };

        clientPair.appClient.createWidget(1, superchart);
        clientPair.appClient.verifyResult(ok(1));

        ValueDisplay valueDisplay = new ValueDisplay();
        valueDisplay.id = 101;
        valueDisplay.height = 2;
        valueDisplay.width = 2;
        valueDisplay.pin = 44;
        valueDisplay.pinType = VIRTUAL;

        clientPair.appClient.createWidget(1, valueDisplay);
        clientPair.appClient.verifyResult(ok(2));

        ValueDisplay valueDisplay2 = new ValueDisplay();
        valueDisplay2.id = 102;
        valueDisplay2.height = 2;
        valueDisplay2.width = 2;
        valueDisplay2.pin = 45;
        valueDisplay2.pinType = VIRTUAL;

        clientPair.appClient.createWidget(1, valueDisplay2);
        clientPair.appClient.verifyResult(ok(3));

        clientPair.hardwareClient.send("hardware vw 44 123");
        clientPair.hardwareClient.send("hardware vw 45 124");
        clientPair.appClient.verifyResult(hardware(2, "1-0 vw 45 124"));

        HttpGet request = new HttpGet(httpServerUrl + clientPair.token + "/get/v45");

        try (CloseableHttpResponse response = httpclient.execute(request)) {
            assertEquals(200, response.getStatusLine().getStatusCode());
            List<String> values = TestUtil.consumeJsonPinValues(response);
            assertEquals(1, values.size());
            assertEquals("124", values.get(0));
        }
    }

    @Test
    public void webhookPinsOverlapsWithOtherWidgets() throws Exception {
        WebHook webHook = new WebHook();
        webHook.id = 100;
        webHook.width = 2;
        webHook.height = 2;
        webHook.pin = 44;
        webHook.pinType = VIRTUAL;

        clientPair.appClient.createWidget(1, webHook);
        clientPair.appClient.verifyResult(ok(1));

        ValueDisplay valueDisplay = new ValueDisplay();
        valueDisplay.id = 101;
        valueDisplay.height = 2;
        valueDisplay.width = 2;
        valueDisplay.pin = 44;
        valueDisplay.pinType = VIRTUAL;

        clientPair.appClient.createWidget(1, valueDisplay);
        clientPair.appClient.verifyResult(ok(2));

        clientPair.hardwareClient.send("hardware vw 44 123");

        HttpGet request = new HttpGet(httpServerUrl + clientPair.token + "/get/v44");

        try (CloseableHttpResponse response = httpclient.execute(request)) {
            assertEquals(200, response.getStatusLine().getStatusCode());
            List<String> values = TestUtil.consumeJsonPinValues(response);
            assertEquals(1, values.size());
            assertEquals("123", values.get(0));
        }
    }
}
