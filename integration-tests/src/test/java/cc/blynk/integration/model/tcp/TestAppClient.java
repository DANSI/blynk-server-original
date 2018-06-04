package cc.blynk.integration.model.tcp;

import cc.blynk.client.handlers.decoders.AppClientMessageDecoder;
import cc.blynk.integration.BaseTest;
import cc.blynk.integration.model.SimpleClientHandler;
import cc.blynk.server.core.model.DashBoard;
import cc.blynk.server.core.model.Profile;
import cc.blynk.server.core.model.auth.App;
import cc.blynk.server.core.model.device.Device;
import cc.blynk.server.core.model.device.Tag;
import cc.blynk.server.core.model.serialization.JsonParser;
import cc.blynk.server.core.model.widgets.Widget;
import cc.blynk.server.core.model.widgets.outputs.graph.GraphPeriod;
import cc.blynk.server.core.model.widgets.ui.reporting.Report;
import cc.blynk.server.core.model.widgets.ui.tiles.TileTemplate;
import cc.blynk.server.core.protocol.handlers.encoders.AppMessageEncoder;
import cc.blynk.server.core.protocol.model.messages.BinaryMessage;
import cc.blynk.server.core.protocol.model.messages.MessageBase;
import cc.blynk.server.core.protocol.model.messages.StringMessage;
import cc.blynk.server.core.stats.GlobalStats;
import cc.blynk.utils.SHA256Util;
import cc.blynk.utils.properties.ServerProperties;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.StringJoiner;

import static cc.blynk.server.core.protocol.enums.Command.GET_PROJECT_BY_CLONE_CODE;
import static cc.blynk.server.core.protocol.enums.Command.GET_PROJECT_BY_TOKEN;
import static cc.blynk.server.core.protocol.enums.Command.GET_PROVISION_TOKEN;
import static cc.blynk.server.core.protocol.enums.Command.LOAD_PROFILE_GZIPPED;
import static cc.blynk.utils.StringUtils.BODY_SEPARATOR;
import static cc.blynk.utils.StringUtils.BODY_SEPARATOR_STRING;
import static cc.blynk.utils.StringUtils.DEVICE_SEPARATOR;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 1/31/2015.
 */
public class TestAppClient extends BaseTestAppClient {

    private int msgId = 0;

    public TestAppClient(String host, int port) {
        super(host, port, Mockito.mock(Random.class), new ServerProperties(Collections.emptyMap()));
    }

    public TestAppClient(String host, int port, ServerProperties properties) {
        this(host, port, properties, new NioEventLoopGroup());
    }

    public TestAppClient(String host, int port, ServerProperties properties, NioEventLoopGroup nioEventLoopGroup) {
        super(host, port, Mockito.mock(Random.class), properties);
        this.nioEventLoopGroup = nioEventLoopGroup;
    }

    public Device getDevice() throws Exception {
        return getDevice(1);
    }

    public Profile getProfile(int expectedMessageOrder) throws Exception {
        return JsonParser.parseProfileFromString(getBody(expectedMessageOrder));
    }

    public Profile getProfile() throws Exception {
        return getProfile(1);
    }

    public Device getDevice(int expectedMessageOrder) throws Exception {
        return JsonParser.parseDevice(getBody(expectedMessageOrder), 0);
    }

    public Device[] getDevices() throws Exception {
        return getDevices(1);
    }

    public Device[] getDevices(int expectedMessageOrder) throws Exception {
        return JsonParser.MAPPER.readValue(getBody(expectedMessageOrder), Device[].class);
    }

    public App getApp(int expectedMessageOrder) throws Exception {
        return JsonParser.parseApp(getBody(expectedMessageOrder), 0);
    }

    public App getApp() throws Exception {
        return getApp(1);
    }

    public DashBoard getDash() throws Exception {
        return getDash(1);
    }

    public DashBoard getDash(int expectedMessageOrder) throws Exception {
        return JsonParser.parseDashboard(getBody(expectedMessageOrder), 0);
    }

    public String getBody() throws Exception {
        return getBody(1);
    }

    public BinaryMessage getBinaryBody() throws Exception {
        ArgumentCaptor<BinaryMessage> objectArgumentCaptor = ArgumentCaptor.forClass(BinaryMessage.class);
        verify(responseMock, timeout(1000)).channelRead(any(), objectArgumentCaptor.capture());
        return objectArgumentCaptor.getValue();
    }

    public String getBody(int expectedMessageOrder) throws Exception {
        ArgumentCaptor<MessageBase> objectArgumentCaptor = ArgumentCaptor.forClass(MessageBase.class);
        verify(responseMock, timeout(1000).times(expectedMessageOrder)).channelRead(any(), objectArgumentCaptor.capture());
        List<MessageBase> arguments = objectArgumentCaptor.getAllValues();
        MessageBase messageBase = arguments.get(expectedMessageOrder - 1);
        if (messageBase instanceof StringMessage) {
            return ((StringMessage) messageBase).body;
        } else if (messageBase.command == LOAD_PROFILE_GZIPPED
                || messageBase.command == GET_PROJECT_BY_TOKEN
                || messageBase.command == GET_PROVISION_TOKEN
                || messageBase.command == GET_PROJECT_BY_CLONE_CODE) {
            return new String(BaseTest.decompress(messageBase.getBytes()));
        }

        throw new RuntimeException("Unexpected message");
    }

    @Override
    public ChannelInitializer<SocketChannel> getChannelInitializer() {
        return new ChannelInitializer<SocketChannel>() {
            @Override
            protected void initChannel(SocketChannel ch) throws Exception {
                ch.pipeline().addLast(
                        sslCtx.newHandler(ch.alloc(), host, port),
                        new AppClientMessageDecoder(),
                        new AppMessageEncoder(new GlobalStats()),
                        responseMock
                );
            }
        };
    }

    public void createTag(int dashId, Tag tag) {
        send("createTag " + dashId + BODY_SEPARATOR + tag.toString());
    }

    public void updateTag(int dashId, Tag tag) {
        send("updateTag " + dashId + BODY_SEPARATOR + tag.toString());
    }

    public void createDevice(int dashId, Device device) {
        send("createDevice " + dashId + BODY_SEPARATOR + device.toString());
    }

    public void updateDevice(int dashId, Device device) {
        send("updateDevice " + dashId + BODY_SEPARATOR + device.toString());
    }

    public void deleteDevice(int dashId, int deviceId) {
        send("deleteDevice " + dashId + BODY_SEPARATOR + deviceId);
    }

    public void createWidget(int dashId, Widget widget) throws Exception {
        createWidget(dashId, JsonParser.MAPPER.writeValueAsString(widget));
    }

    public void createWidget(int dashId, long widgetId, long templateId, String widgetJson) {
        send("createWidget " + dashId + BODY_SEPARATOR + widgetId
                + BODY_SEPARATOR + templateId + BODY_SEPARATOR + widgetJson);
    }

    public void createWidget(int dashId, long widgetId, long templateId, Widget widget) throws Exception {
        send("createWidget " + dashId + BODY_SEPARATOR + widgetId
                + BODY_SEPARATOR + templateId + BODY_SEPARATOR + JsonParser.MAPPER.writeValueAsString(widget));
    }

    public void createWidget(int dashId, String widgetJson) {
        send("createWidget " + dashId + BODY_SEPARATOR + widgetJson);
    }

    public void updateWidget(int dashId, Widget widget) throws Exception {
        updateWidget(dashId, JsonParser.MAPPER.writeValueAsString(widget));
    }

    public void updateWidget(int dashId, String widgetJson) {
        send("updateWidget " + dashId + BODY_SEPARATOR + widgetJson);
    }

    public void deleteWidget(int dashId, long widgetId) {
        send("deleteWidget " + dashId + " " + widgetId);
    }

    public void activate(int dashId) {
        send("activate " + dashId);
    }

    public void deactivate(int dashId) {
        send("deactivate " + dashId);
    }

    public void updateDash(DashBoard dashBoard) {
        updateDash(dashBoard.toString());
    }

    public void updateDash(String dashJson) {
        send("updateDash " + dashJson);
    }

    public void deleteDash(int dashId) {
        send("deleteDash " + dashId);
    }

    public void deleteTag(int dashId, int tagId) {
        send("deleteTag " + dashId + BODY_SEPARATOR + tagId);
    }

    public void createDash(DashBoard dashBoard) {
        createDash(dashBoard.toString());
    }

    public void createDash(String dashJson) {
        send("createDash " + dashJson);
    }

    public void register(String email, String pass) {
        send("register " + email + BODY_SEPARATOR + SHA256Util.makeHash(pass, email));
    }

    public void register(String email, String pass, String appName) {
        send("register " + email + BODY_SEPARATOR + SHA256Util.makeHash(pass, email) + BODY_SEPARATOR + appName);
    }

    public void login(String email, String pass) {
        send("login " + email + BODY_SEPARATOR + SHA256Util.makeHash(pass, email));
    }

    public void login(String email, String pass, String os, String version) {
        send("login " + email + BODY_SEPARATOR + SHA256Util.makeHash(pass, email)
                + BODY_SEPARATOR + os + BODY_SEPARATOR + version);
    }

    public void login(String email, String pass, String os, String version, String appName) {
        send("login " + email + BODY_SEPARATOR + SHA256Util.makeHash(pass, email)
                + BODY_SEPARATOR + os + BODY_SEPARATOR + version + BODY_SEPARATOR + appName);
    }

    public void sync(int dashId) {
        send("appsync " + dashId);
    }

    public void sync(int dashId, int deviceId) {
        send("appsync " + dashId + DEVICE_SEPARATOR + deviceId);
    }

    public void getToken(int dashId, int deviceId) {
        send("getToken " + dashId + DEVICE_SEPARATOR + deviceId);
    }

    public void getToken(int dashId) {
        send("getToken " + dashId);
    }

    public void deleteDeviceData(int dashId, int deviceId) {
        send("deletedevicedata " + dashId + DEVICE_SEPARATOR + deviceId);
    }

    public void deleteDeviceData(int dashId, int deviceId, String... pins) {
        StringJoiner sj = new StringJoiner(BODY_SEPARATOR_STRING);
        for (String pin : pins) {
            sj.add(pin);
        }
        send("deletedevicedata " + dashId + DEVICE_SEPARATOR + deviceId + BODY_SEPARATOR + sj.toString());
    }

    public void getEnhancedGraphData(int dashId, long widgetId, GraphPeriod period) {
        send("getenhanceddata " + dashId + BODY_SEPARATOR + widgetId + BODY_SEPARATOR + period.name());
    }

    public void getEnhancedGraphData(int dashId, long widgetId, GraphPeriod period, int page) {
        send("getenhanceddata " + dashId + BODY_SEPARATOR + widgetId + BODY_SEPARATOR + period.name() + BODY_SEPARATOR + page);
    }

    public void createTemplate(int dashId, long widgetId, TileTemplate tileTemplate) throws Exception {
        createTemplate(dashId, widgetId, JsonParser.MAPPER.writeValueAsString(tileTemplate));
    }

    public void createTemplate(int dashId, long widgetId, String tileTemplate) throws Exception {
        send("createTemplate " + dashId + BODY_SEPARATOR + widgetId + BODY_SEPARATOR + tileTemplate);
    }

    public void updateTemplate(int dashId, long widgetId, TileTemplate tileTemplate) throws Exception {
        send("updateTemplate " + dashId + BODY_SEPARATOR + widgetId + BODY_SEPARATOR
                + JsonParser.MAPPER.writeValueAsString(tileTemplate));
    }

    public void createReport(int dashId, Report report) {
        send("createReport " + dashId + BODY_SEPARATOR + report.toString());
    }

    public void updateReport(int dashId, Report report) {
        send("updateReport " + dashId + BODY_SEPARATOR + report.toString());
    }

    public void deleteReport(int dashId, int reportId) {
        send("deleteReport " + dashId + BODY_SEPARATOR + reportId);
    }

    public void exportReport(int dashId, int reportId) {
        send("exportReport " + dashId + BODY_SEPARATOR + reportId);
    }

    public void send(String line) {
        send(produceMessageBaseOnUserInput(line, ++msgId));
    }

    public void send(String line, int id) {
        send(produceMessageBaseOnUserInput(line, id));
    }

    public void reset() {
        Mockito.reset(responseMock);
        msgId = 0;
    }

    public void replace(SimpleClientHandler simpleClientHandler) {
        this.channel.pipeline().removeLast();
        this.channel.pipeline().addLast(simpleClientHandler);
    }

}
