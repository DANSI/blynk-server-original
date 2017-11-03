package cc.blynk.integration.http;

import cc.blynk.integration.IntegrationBase;
import cc.blynk.integration.model.tcp.ClientPair;
import cc.blynk.integration.model.tcp.TestHardClient;
import cc.blynk.integration.tcp.EventorTest;
import cc.blynk.server.api.http.HttpAPIServer;
import cc.blynk.server.application.AppServer;
import cc.blynk.server.core.BaseServer;
import cc.blynk.server.core.model.DataStream;
import cc.blynk.server.core.model.device.Device;
import cc.blynk.server.core.model.enums.PinType;
import cc.blynk.server.core.model.serialization.JsonParser;
import cc.blynk.server.core.model.widgets.controls.RGB;
import cc.blynk.server.core.model.widgets.controls.Timer;
import cc.blynk.server.core.model.widgets.others.eventor.Eventor;
import cc.blynk.server.core.model.widgets.others.eventor.Rule;
import cc.blynk.server.core.model.widgets.others.eventor.TimerTime;
import cc.blynk.server.core.model.widgets.others.eventor.model.action.BaseAction;
import cc.blynk.server.core.model.widgets.others.eventor.model.action.SetPinAction;
import cc.blynk.server.core.model.widgets.others.eventor.model.action.SetPinActionType;
import cc.blynk.server.core.model.widgets.others.rtc.RTC;
import cc.blynk.server.core.model.widgets.ui.table.Table;
import cc.blynk.server.core.protocol.model.messages.ResponseMessage;
import cc.blynk.server.core.protocol.model.messages.appllication.CreateDevice;
import cc.blynk.server.hardware.HardwareServer;
import cc.blynk.utils.DateTimeUtils;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static cc.blynk.server.core.protocol.enums.Command.HARDWARE;
import static cc.blynk.server.core.protocol.enums.Response.OK;
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
public class HttpAndTCPSameJVMTest extends IntegrationBase {

    private BaseServer httpServer;
    private BaseServer hardwareServer;
    private BaseServer appServer;

    private CloseableHttpClient httpclient;
    private String httpServerUrl;

    private ClientPair clientPair;

    @After
    public void shutdown() throws Exception {
        httpclient.close();
        httpServer.close();
        hardwareServer.close();
        appServer.close();
        clientPair.stop();
    }

    @Before
    public void init() throws Exception {
        httpServer = new HttpAPIServer(holder).start();
        hardwareServer = new HardwareServer(holder).start();
        appServer = new AppServer(holder).start();
        httpServerUrl = String.format("http://localhost:%s/", httpPort);
        httpclient = HttpClients.createDefault();
        clientPair = initAppAndHardPair(tcpAppPort, tcpHardPort, properties);
        clientPair.hardwareClient.reset();
        clientPair.appClient.reset();
    }

    @Test
    @Ignore
    //todo fix? - https://github.com/blynkkk/blynk-server/issues/754
    public void testWrongPortHttpConnect() throws Exception {
        HttpGet request = new HttpGet(String.format("http://localhost:%s/", tcpHardPort) + "robots.txt");

        try (CloseableHttpResponse response = httpclient.execute(request)) {
            assertEquals(200, response.getStatusLine().getStatusCode());
        }
    }

    @Test
    public void testChangeNonWidgetPinValueViaHardwareAndGetViaHTTP() throws Exception {
        clientPair.hardwareClient.send("hardware vw 10 200");
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(produce(1, HARDWARE, b("1 vw 10 200"))));

        reset(clientPair.appClient.responseMock);

        clientPair.appClient.send("getToken 1");
        String token = clientPair.appClient.getBody();

        HttpGet request = new HttpGet(httpServerUrl + token + "/pin/v10");

        try (CloseableHttpResponse response = httpclient.execute(request)) {
            assertEquals(200, response.getStatusLine().getStatusCode());
            List<String> values = consumeJsonPinValues(response);
            assertEquals(1, values.size());
            assertEquals("200", values.get(0));
        }

        clientPair.appClient.send("hardware 1 vw 10 201");
        verify(clientPair.hardwareClient.responseMock, timeout(500)).channelRead(any(), eq(produce(2, HARDWARE, b("vw 10 201"))));

        try (CloseableHttpResponse response = httpclient.execute(request)) {
            assertEquals(200, response.getStatusLine().getStatusCode());
            List<String> values = consumeJsonPinValues(response);
            assertEquals(1, values.size());
            assertEquals("201", values.get(0));
        }
    }

    @Test
    public void testChangePinValueViaAppAndHardware() throws Exception {
        clientPair.hardwareClient.send("hardware vw 4 200");
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(produce(1, HARDWARE, b("1 vw 4 200"))));

        reset(clientPair.appClient.responseMock);

        clientPair.appClient.send("getToken 1");
        String token = clientPair.appClient.getBody();

        HttpGet request = new HttpGet(httpServerUrl + token + "/pin/v4");

        try (CloseableHttpResponse response = httpclient.execute(request)) {
            assertEquals(200, response.getStatusLine().getStatusCode());
            List<String> values = consumeJsonPinValues(response);
            assertEquals(1, values.size());
            assertEquals("200", values.get(0));
        }

        clientPair.appClient.send("hardware 1 vw 4 201");
        verify(clientPair.hardwareClient.responseMock, timeout(500)).channelRead(any(), eq(produce(2, HARDWARE, b("vw 4 201"))));

        try (CloseableHttpResponse response = httpclient.execute(request)) {
            assertEquals(200, response.getStatusLine().getStatusCode());
            List<String> values = consumeJsonPinValues(response);
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

        clientPair.appClient.send("createWidget 1\0" + JsonParser.MAPPER.writeValueAsString(rtc));
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(ok(1)));

        reset(clientPair.appClient.responseMock);

        clientPair.appClient.send("getToken 1");
        String token = clientPair.appClient.getBody();

        HttpGet request = new HttpGet(httpServerUrl + token + "/rtc");

        try (CloseableHttpResponse response = httpclient.execute(request)) {
            assertEquals(200, response.getStatusLine().getStatusCode());
            List<String> values = consumeJsonPinValues(response);
            assertEquals(1, values.size());
        }
    }

    @Test
    public void testEventorWorksViaHttpAPI() throws Exception {
        Eventor eventor = EventorTest.oneRuleEventor("if v100 = 37 then setpin v2 123");
        eventor.height = 1;
        eventor.width = 2;

        clientPair.appClient.send("createWidget 1\0" + JsonParser.MAPPER.writeValueAsString(eventor));
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(ok(1)));

        reset(clientPair.appClient.responseMock);

        clientPair.appClient.send("getToken 1");
        String token = clientPair.appClient.getBody();

        HttpPut request = new HttpPut(httpServerUrl + token + "/pin/v100");
        request.setEntity(new StringEntity("[\"37\"]", ContentType.APPLICATION_JSON));

        try (CloseableHttpResponse response = httpclient.execute(request)) {
            assertEquals(200, response.getStatusLine().getStatusCode());
        }

        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(produce(111, HARDWARE, b("1 vw 100 37"))));
        verify(clientPair.hardwareClient.responseMock, timeout(500)).channelRead(any(), eq(produce(111, HARDWARE, b("vw 100 37"))));
        verify(clientPair.hardwareClient.responseMock, timeout(500)).channelRead(any(), eq(produce(888, HARDWARE, b("vw 2 123"))));
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

        DataStream dataStream = new DataStream((byte) 4, PinType.VIRTUAL);
        SetPinAction setPinAction = new SetPinAction(dataStream, "1", SetPinActionType.CUSTOM);

        Eventor eventor = new Eventor(new Rule[] {
                new Rule(dataStream, timerTime, null, new BaseAction[] {setPinAction}, true)
        });
        eventor.id = 1000;

        clientPair.appClient.send("createWidget 1\0" + JsonParser.MAPPER.writeValueAsString(eventor));
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(ok(1)));

        verify(clientPair.appClient.responseMock, timeout(2000)).channelRead(any(), eq(produce(TIMER_MSG_ID, HARDWARE, b("1 vw 4 1"))));
        verify(clientPair.hardwareClient.responseMock, timeout(2000)).channelRead(any(), eq(produce(TIMER_MSG_ID, HARDWARE, b("vw 4 1"))));



        clientPair.appClient.reset();
        clientPair.appClient.send("getToken 1");
        String token = clientPair.appClient.getBody();

        HttpGet requestGET = new HttpGet(httpServerUrl + token + "/pin/v4");

        try (CloseableHttpResponse response = httpclient.execute(requestGET)) {
            assertEquals(200, response.getStatusLine().getStatusCode());
            List<String> values = consumeJsonPinValues(response);
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
        timer.pinType = PinType.VIRTUAL;
        timer.pin = 4;
        timer.width = 2;
        timer.height = 1;
        timer.startValue = "1";
        timer.stopValue = "0";
        LocalTime localDateTime = LocalTime.now(ZoneId.of("UTC"));
        int curTime = localDateTime.toSecondOfDay();
        timer.startTime = curTime + 1;
        timer.stopTime = curTime + 1;

        clientPair.appClient.send("createWidget 1\0" + JsonParser.toJson(timer));
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(new ResponseMessage(1, OK)));

        verify(clientPair.hardwareClient.responseMock, timeout(2500).times(2)).channelRead(any(), any());
        verify(clientPair.hardwareClient.responseMock, timeout(2000)).channelRead(any(), eq(produce(7777, HARDWARE, b("vw 4 1"))));
        verify(clientPair.hardwareClient.responseMock, timeout(2000)).channelRead(any(), eq(produce(7777, HARDWARE, b("vw 4 0"))));

        clientPair.appClient.reset();
        clientPair.appClient.send("getToken 1");
        String token = clientPair.appClient.getBody();

        HttpGet requestGET = new HttpGet(httpServerUrl + token + "/pin/v4");

        try (CloseableHttpResponse response = httpclient.execute(requestGET)) {
            assertEquals(200, response.getStatusLine().getStatusCode());
            List<String> values = consumeJsonPinValues(response);
            assertEquals(1, values.size());
            //todo order is not guarateed here!!! Known issue
            String res = values.get(0);
            assertTrue("0".equals(res) || "1".equals(res));
        }
    }

    @Test
    public void testChangePinValueViaAppAndHardwareForWrongPWMButton() throws Exception {
        clientPair.appClient.send("createWidget 1\0{\"type\":\"BUTTON\",\"id\":1000,\"x\":0,\"y\":0,\"color\":616861439,\"width\":2,\"height\":2,\"label\":\"Relay\",\"pinType\":\"DIGITAL\",\"pin\":18,\"pwmMode\":true,\"rangeMappingOn\":false,\"min\":0,\"max\":0,\"value\":\"1\",\"pushMode\":false}");
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(new ResponseMessage(1, OK)));

        clientPair.appClient.reset();
        clientPair.appClient.send("getToken 1");
        String token = clientPair.appClient.getBody();

        HttpGet requestGET = new HttpGet(httpServerUrl + token + "/pin/d18");

        try (CloseableHttpResponse response = httpclient.execute(requestGET)) {
            assertEquals(200, response.getStatusLine().getStatusCode());
            List<String> values = consumeJsonPinValues(response);
            assertEquals(1, values.size());
            assertEquals("1", values.get(0));
        }

        HttpPut requestPUT = new HttpPut(httpServerUrl + token + "/pin/d18");
        requestPUT.setEntity(new StringEntity("[\"0\"]", ContentType.APPLICATION_JSON));

        try (CloseableHttpResponse response = httpclient.execute(requestPUT)) {
            assertEquals(200, response.getStatusLine().getStatusCode());
        }

        verify(clientPair.hardwareClient.responseMock, timeout(500)).channelRead(any(), eq(produce(111, HARDWARE, b("dw 18 0"))));
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(produce(111, HARDWARE, b("1 dw 18 0"))));
    }

    @Test
    public void testChangePinValueViaHttpAPI() throws Exception {
        clientPair.appClient.send("getToken 1");
        String token = clientPair.appClient.getBody();

        HttpPut request = new HttpPut(httpServerUrl + token + "/pin/v4");
        HttpGet getRequest = new HttpGet(httpServerUrl + token + "/pin/v4");

        for (int i = 0; i < 50; i++) {
            request.setEntity(new StringEntity("[\"" + i + "\"]", ContentType.APPLICATION_JSON));
            try (CloseableHttpResponse response = httpclient.execute(request)) {
                assertEquals(200, response.getStatusLine().getStatusCode());
            }

            clientPair.hardwareClient.send("hardsync " + b("vr 4"));
            verify(clientPair.hardwareClient.responseMock, timeout(500)).channelRead(any(), eq(produce(i + 1, HARDWARE, b("vw 4 " + i))));

            try (CloseableHttpResponse response = httpclient.execute(getRequest)) {
                assertEquals(200, response.getStatusLine().getStatusCode());
                List<String> values = consumeJsonPinValues(response);
                assertEquals(1, values.size());
                assertEquals(i, Integer.valueOf(values.get(0)).intValue());
            }
        }
    }

    @Test
    public void testIsHardwareAndAppConnected() throws Exception {
        clientPair.appClient.send("getToken 1");
        String token = clientPair.appClient.getBody();

        HttpGet request = new HttpGet(httpServerUrl + token + "/isHardwareConnected");

        try (CloseableHttpResponse response = httpclient.execute(request)) {
            assertEquals(200, response.getStatusLine().getStatusCode());
            String value = consumeText(response);
            assertNotNull(value);
            assertEquals("true", value);
        }

        request = new HttpGet(httpServerUrl + token + "/isAppConnected");

        try (CloseableHttpResponse response = httpclient.execute(request)) {
            assertEquals(200, response.getStatusLine().getStatusCode());
            String value = consumeText(response);
            assertNotNull(value);
            assertEquals("true", value);
        }
    }

    @Test
    public void testIsHardwareAndAppDisconnected() throws Exception {
        clientPair.appClient.send("getToken 1");
        String token = clientPair.appClient.getBody();

        clientPair.stop();

        HttpGet request = new HttpGet(httpServerUrl + token + "/isHardwareConnected");

        try (CloseableHttpResponse response = httpclient.execute(request)) {
            assertEquals(200, response.getStatusLine().getStatusCode());
            String value = consumeText(response);
            assertNotNull(value);
            assertEquals("false", value);
        }

        request = new HttpGet(httpServerUrl + token + "/isAppConnected");

        try (CloseableHttpResponse response = httpclient.execute(request)) {
            assertEquals(200, response.getStatusLine().getStatusCode());
            String value = consumeText(response);
            assertNotNull(value);
            assertEquals("false", value);
        }

        clientPair = initAppAndHardPair(tcpAppPort, tcpHardPort, properties);
    }

    @Test
    public void testIsHardwareConnecteedWithMultiDevices() throws Exception {
        clientPair.appClient.send("getToken 1");
        String token = clientPair.appClient.getBody();

        HttpGet request = new HttpGet(httpServerUrl + token + "/isHardwareConnected");

        try (CloseableHttpResponse response = httpclient.execute(request)) {
            assertEquals(200, response.getStatusLine().getStatusCode());
            String value = consumeText(response);
            assertNotNull(value);
            assertEquals("true", value);
        }

        request = new HttpGet(httpServerUrl + token + "/isAppConnected");

        try (CloseableHttpResponse response = httpclient.execute(request)) {
            assertEquals(200, response.getStatusLine().getStatusCode());
            String value = consumeText(response);
            assertNotNull(value);
            assertEquals("true", value);
        }

        Device device1 = new Device(1, "My Device", "ESP8266");

        clientPair.appClient.send("createDevice 1\0" + device1.toString());
        String createdDevice = clientPair.appClient.getBody(2);
        Device device = JsonParser.parseDevice(createdDevice);
        assertNotNull(device);
        assertNotNull(device.token);
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(new CreateDevice(2, device.toString())));

        clientPair.appClient.reset();

        clientPair.appClient.send("getDevices 1");
        String deviceResponse = clientPair.appClient.getBody();

        Device[] devices = JsonParser.MAPPER.readValue(deviceResponse, Device[].class);
        assertNotNull(devices);
        assertEquals(2, devices.length);

        TestHardClient hardClient2 = new TestHardClient("localhost", tcpHardPort);
        hardClient2.start();

        hardClient2.send("login " + devices[1].token);
        verify(hardClient2.responseMock, timeout(500)).channelRead(any(), eq(new ResponseMessage(1, OK)));

        clientPair.stop();

        request = new HttpGet(httpServerUrl + token + "/isHardwareConnected");

        try (CloseableHttpResponse response = httpclient.execute(request)) {
            assertEquals(200, response.getStatusLine().getStatusCode());
            String value = consumeText(response);
            assertNotNull(value);
            assertEquals("false", value);
        }

        request = new HttpGet(httpServerUrl + token + "/isAppConnected");

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
        clientPair.appClient.send("getToken 1");
        String token = clientPair.appClient.getBody();
        clientPair.appClient.reset();

        clientPair.appClient.send("deactivate 1");
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(new ResponseMessage(1, OK)));

        HttpPut request = new HttpPut(httpServerUrl + token + "/pin/v31");

        request.setEntity(new StringEntity("[\"100\"]", ContentType.APPLICATION_JSON));
        try (CloseableHttpResponse response = httpclient.execute(request)) {
            assertEquals(200, response.getStatusLine().getStatusCode());
        }

        verify(clientPair.hardwareClient.responseMock, timeout(500)).channelRead(any(), eq(produce(111, HARDWARE, b("vw 31 100"))));
        verify(clientPair.appClient.responseMock, after(400).never()).channelRead(any(), eq(produce(111, HARDWARE, b("1 vw 31 100"))));

        clientPair.appClient.send("activate 1");
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(new ResponseMessage(2, OK)));
    }

    @Test
    public void testChangeLCDPinValueViaHttpAPIAndValueChanged() throws Exception {
        clientPair.appClient.send("getToken 1");
        String token = clientPair.appClient.getBody();

        HttpPut request = new HttpPut(httpServerUrl + token + "/pin/v0");

        request.setEntity(new StringEntity("[\"100\"]", ContentType.APPLICATION_JSON));
        try (CloseableHttpResponse response = httpclient.execute(request)) {
            assertEquals(200, response.getStatusLine().getStatusCode());
        }

        verify(clientPair.hardwareClient.responseMock, timeout(500)).channelRead(any(), eq(produce(111, HARDWARE, b("vw 0 100"))));
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(produce(111, HARDWARE, b("1 vw 0 100"))));

        request = new HttpPut(httpServerUrl + token + "/pin/v1");

        request.setEntity(new StringEntity("[\"101\"]", ContentType.APPLICATION_JSON));
        try (CloseableHttpResponse response = httpclient.execute(request)) {
            assertEquals(200, response.getStatusLine().getStatusCode());
        }

        verify(clientPair.hardwareClient.responseMock, timeout(500)).channelRead(any(), eq(produce(111, HARDWARE, b("vw 1 101"))));
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(produce(111, HARDWARE, b("1 vw 1 101"))));
    }

    @Test
    public void testChangePinValueViaHttpAPIAndNoWidgetSinglePinValue() throws Exception {
        clientPair.appClient.send("getToken 1");
        String token = clientPair.appClient.getBody();

        HttpPut request = new HttpPut(httpServerUrl + token + "/pin/v31");

        request.setEntity(new StringEntity("[\"100\"]", ContentType.APPLICATION_JSON));
        try (CloseableHttpResponse response = httpclient.execute(request)) {
            assertEquals(200, response.getStatusLine().getStatusCode());
        }

        verify(clientPair.hardwareClient.responseMock, timeout(500)).channelRead(any(), eq(produce(111, HARDWARE, b("vw 31 100"))));
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(produce(111, HARDWARE, b("1 vw 31 100"))));
    }

    @Test
    public void testChangePinValueViaHttpAPIAndForTerminal() throws Exception {
        clientPair.appClient.send("getToken 1");
        String token = clientPair.appClient.getBody();

        clientPair.appClient.send("createWidget 1\0{\"id\":222, \"width\":1, \"height\":1, \"x\":2, \"y\":2, \"label\":\"Some Text 2\", \"type\":\"TERMINAL\", \"pinType\":\"VIRTUAL\", \"pin\":100}");
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(new ResponseMessage(2, OK)));

        HttpPut request = new HttpPut(httpServerUrl + token + "/pin/V100");

        request.setEntity(new StringEntity("[\"100\"]", ContentType.APPLICATION_JSON));
        try (CloseableHttpResponse response = httpclient.execute(request)) {
            assertEquals(200, response.getStatusLine().getStatusCode());
        }

        verify(clientPair.hardwareClient.responseMock, timeout(500)).channelRead(any(), eq(produce(111, HARDWARE, b("vw 100 100"))));
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(produce(111, HARDWARE, b("1 vw 100 100"))));
    }

    @Test
    public void testChangePinValueViaHttpAPIAndNoWidgetMultiPinValue() throws Exception {
        clientPair.appClient.send("getToken 1");
        String token = clientPair.appClient.getBody();

        HttpPut request = new HttpPut(httpServerUrl + token + "/pin/v31");

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
        table.pinType = PinType.VIRTUAL;
        table.isClickableRows = true;
        table.isReoderingAllowed = true;
        table.height = 2;
        table.width = 2;

        clientPair.appClient.send("createWidget 1\0" + JsonParser.MAPPER.writeValueAsString(table));
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(ok(1)));

        clientPair.appClient.send("getToken 1");
        String token = clientPair.appClient.getBody(2);
        HttpGet updateTableRow = new HttpGet(httpServerUrl + token + "/update/v123?value=add&value=2&value=Martes&value=120Kwh");
        try (CloseableHttpResponse response = httpclient.execute(updateTableRow)) {
            assertEquals(200, response.getStatusLine().getStatusCode());
        }
    }

    @Test
    public void sendMultiValueToAppViaHttpApi() throws Exception {
        clientPair.appClient.send("getToken 1");
        String token = clientPair.appClient.getBody();

        HttpGet updateTableRow = new HttpGet(httpServerUrl + token + "/update/V1?value=110&value=230&value=330");
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
                new DataStream((byte) 101, PinType.VIRTUAL)
        };
        rgb.splitMode = false;
        rgb.height = 2;
        rgb.width = 2;

        clientPair.appClient.send("createWidget 1\0" + JsonParser.MAPPER.writeValueAsString(rgb));
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(ok(1)));

        clientPair.appClient.send("getToken 1");
        String token = clientPair.appClient.getBody(2);

        HttpGet updateTableRow = new HttpGet(httpServerUrl + token + "/update/V101?value=110&value=230&value=330");
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
                new DataStream((byte) 101, PinType.VIRTUAL),
                new DataStream((byte) 102, PinType.VIRTUAL),
                new DataStream((byte) 103, PinType.VIRTUAL)
        };
        rgb.splitMode = false;
        rgb.height = 2;
        rgb.width = 2;

        clientPair.appClient.send("createWidget 1\0" + JsonParser.MAPPER.writeValueAsString(rgb));
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(ok(1)));

        clientPair.appClient.send("getToken 1");
        String token = clientPair.appClient.getBody(2);

        HttpGet updateTableRow = new HttpGet(httpServerUrl + token + "/update/V101?value=110&value=230&value=330");
        try (CloseableHttpResponse response = httpclient.execute(updateTableRow)) {
            assertEquals(200, response.getStatusLine().getStatusCode());
        }

        verify(clientPair.hardwareClient.responseMock, timeout(500)).channelRead(any(),
                eq(produce(111, HARDWARE, b("vw 101 110 230 330"))));
    }
}
