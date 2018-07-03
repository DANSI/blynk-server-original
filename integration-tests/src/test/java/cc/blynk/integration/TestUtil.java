package cc.blynk.integration;

import cc.blynk.integration.model.SimpleClientHandler;
import cc.blynk.integration.model.tcp.ClientPair;
import cc.blynk.integration.model.tcp.TestAppClient;
import cc.blynk.integration.model.tcp.TestHardClient;
import cc.blynk.server.core.model.DashBoard;
import cc.blynk.server.core.model.DataStream;
import cc.blynk.server.core.model.Profile;
import cc.blynk.server.core.model.device.Device;
import cc.blynk.server.core.model.device.Tag;
import cc.blynk.server.core.model.serialization.JsonParser;
import cc.blynk.server.core.model.widgets.AppSyncWidget;
import cc.blynk.server.core.model.widgets.MultiPinWidget;
import cc.blynk.server.core.model.widgets.OnePinWidget;
import cc.blynk.server.core.model.widgets.Widget;
import cc.blynk.server.core.protocol.model.messages.MessageBase;
import cc.blynk.server.core.protocol.model.messages.ResponseMessage;
import cc.blynk.server.core.protocol.model.messages.StringMessage;
import cc.blynk.server.core.protocol.model.messages.appllication.GetServerMessage;
import cc.blynk.server.core.protocol.model.messages.common.HardwareMessage;
import cc.blynk.utils.StringUtils;
import cc.blynk.utils.properties.ServerProperties;
import com.fasterxml.jackson.databind.ObjectReader;
import org.mockito.ArgumentCaptor;

import java.io.InputStream;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import static cc.blynk.server.core.protocol.enums.Command.APP_SYNC;
import static cc.blynk.server.core.protocol.enums.Command.BLYNK_INTERNAL;
import static cc.blynk.server.core.protocol.enums.Command.BRIDGE;
import static cc.blynk.server.core.protocol.enums.Command.CONNECT_REDIRECT;
import static cc.blynk.server.core.protocol.enums.Command.CREATE_DEVICE;
import static cc.blynk.server.core.protocol.enums.Command.CREATE_TAG;
import static cc.blynk.server.core.protocol.enums.Command.DEVICE_OFFLINE;
import static cc.blynk.server.core.protocol.enums.Command.GET_PROJECT_BY_CLONE_CODE;
import static cc.blynk.server.core.protocol.enums.Command.GET_PROJECT_BY_TOKEN;
import static cc.blynk.server.core.protocol.enums.Command.GET_PROVISION_TOKEN;
import static cc.blynk.server.core.protocol.enums.Command.GET_TOKEN;
import static cc.blynk.server.core.protocol.enums.Command.HARDWARE_CONNECTED;
import static cc.blynk.server.core.protocol.enums.Command.LOAD_PROFILE_GZIPPED;
import static cc.blynk.server.core.protocol.enums.Command.OUTDATED_APP_NOTIFICATION;
import static cc.blynk.server.core.protocol.enums.Command.SET_WIDGET_PROPERTY;
import static cc.blynk.server.core.protocol.enums.Response.ILLEGAL_COMMAND;
import static cc.blynk.server.core.protocol.enums.Response.ILLEGAL_COMMAND_BODY;
import static cc.blynk.server.core.protocol.enums.Response.INVALID_TOKEN;
import static cc.blynk.server.core.protocol.enums.Response.NOT_ALLOWED;
import static cc.blynk.server.core.protocol.enums.Response.OK;
import static cc.blynk.server.core.protocol.enums.Response.SERVER_ERROR;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

public final class TestUtil {

    public static final String DEFAULT_TEST_USER = "dima@mail.ua";
    private static final ObjectReader profileReader = JsonParser.init().readerFor(Profile.class);

    private TestUtil() {
    }

    public static String getBody(SimpleClientHandler responseMock) throws Exception {
        return getBody(responseMock, 1);
    }

    public static String getBody(SimpleClientHandler responseMock, int expectedMessageOrder) throws Exception {
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

    public static Profile parseProfile(InputStream reader) throws Exception {
        return profileReader.readValue(reader);
    }

    public static Profile parseProfile(String reader) throws Exception {
        return profileReader.readValue(reader);
    }


    public static String readTestUserProfile(String fileName) throws Exception{
        if (fileName == null) {
            fileName = "user_profile_json.txt";
        }
        InputStream is = TestUtil.class.getResourceAsStream("/json_test/" + fileName);
        Profile profile = parseProfile(is);
        return profile.toString();
    }

    public static String readTestUserProfile() throws Exception {
        return readTestUserProfile(null);
    }

    protected static StringMessage getGetTokenMessage(List<Object> arguments) {
        for (Object obj : arguments) {
            if (((MessageBase)obj).command == GET_TOKEN) {
                return (StringMessage) obj;
            }
        }
        throw new RuntimeException("Get token message wasn't retrieved.");
    }

    public static void saveProfile(TestAppClient appClient, DashBoard... dashBoards) {
        for (DashBoard dash : dashBoards) {
            appClient.createDash(dash);
        }
    }

    public static String b(String body) {
        return body.replaceAll(" ", StringUtils.BODY_SEPARATOR_STRING);
    }

    public static ResponseMessage illegalCommand(int msgId) {
        return new ResponseMessage(msgId, ILLEGAL_COMMAND);
    }

    public static ResponseMessage illegalCommandBody(int msgId) {
        return new ResponseMessage(msgId, ILLEGAL_COMMAND_BODY);
    }

    public static ResponseMessage ok(int msgId) {
        return new ResponseMessage(msgId, OK);
    }

    public static StringMessage bridge(int msgId, String body) {
        return new StringMessage(msgId, BRIDGE, b(body));
    }

    public static StringMessage internal(int msgId, String body) {
        return new StringMessage(msgId, BLYNK_INTERNAL, b(body));
    }

    public static StringMessage hardwareConnected(int msgId, String body) {
        return new StringMessage(msgId, HARDWARE_CONNECTED, body);
    }

    public static GetServerMessage getServer(int msgId, String body) {
        return new GetServerMessage(msgId, body);
    }

    public static StringMessage deviceOffline(int msgId, String body) {
        return new StringMessage(msgId, DEVICE_OFFLINE, body);
    }

    public static StringMessage createTag(int msgId, Tag tag) {
        return createTag(msgId, tag.toString());
    }

    public static StringMessage createTag(int msgId, String body) {
        return new StringMessage(msgId, CREATE_TAG, body);
    }

    public static StringMessage appIsOutdated(int msgId, String body) {
        return new StringMessage(msgId, OUTDATED_APP_NOTIFICATION, body);
    }

    public static StringMessage appSync(int msgId, String body) {
        return new StringMessage(msgId, APP_SYNC, b(body));
    }

    public static StringMessage hardware(int msgId, String body) {
        return new HardwareMessage(msgId, b(body));
    }

    public static StringMessage appSync(String body) {
        return appSync(AppSyncWidget.SYNC_DEFAULT_MESSAGE_ID, body);
    }

    public static StringMessage setProperty(int msgId, String body) {
        return new StringMessage(msgId, SET_WIDGET_PROPERTY, b(body));
    }

    public static StringMessage createDevice(int msgId, String body) {
        return new StringMessage(msgId, CREATE_DEVICE, body);
    }

    public static StringMessage createDevice(int msgId, Device device) {
        return createDevice(msgId, device.toString());
    }

    public static StringMessage connectRedirect(int msgId, String body) {
        return new StringMessage(msgId, CONNECT_REDIRECT, b(body));
    }

    public static ResponseMessage serverError(int msgId) {
        return new ResponseMessage(msgId, SERVER_ERROR);
    }

    public static ResponseMessage notAllowed(int msgId) {
        return new ResponseMessage(msgId, NOT_ALLOWED);
    }

    public static ResponseMessage invalidToken(int msgId) {
        return new ResponseMessage(msgId, INVALID_TOKEN);
    }

    public static ClientPair initAppAndHardPair(String host, int appPort, int hardPort,
                                                String user, String pass,
                                                String jsonProfile,
                                                ServerProperties properties, int energy) throws Exception {

        TestAppClient appClient = new TestAppClient(host, appPort, properties);
        TestHardClient hardClient = new TestHardClient(host, hardPort);

        return initAppAndHardPair(appClient, hardClient, user, pass, jsonProfile, energy);
    }

    public static ClientPair initAppAndHardPair(TestAppClient appClient, TestHardClient hardClient,
                                                String user, String pass,
                                                String jsonProfile, int energy) throws Exception {

        appClient.start();
        hardClient.start();

        String userProfileString = readTestUserProfile(jsonProfile);
        Profile profile = parseProfile(userProfileString);

        int expectedSyncCommandsCount = 0;
        for (Widget widget : profile.dashBoards[0].widgets) {
            if (widget instanceof OnePinWidget) {
                if (((OnePinWidget) widget).makeHardwareBody() != null) {
                    expectedSyncCommandsCount++;
                }
            } else if (widget instanceof MultiPinWidget) {
                MultiPinWidget multiPinWidget = (MultiPinWidget) widget;
                if (multiPinWidget.dataStreams != null) {
                    if (multiPinWidget.isSplitMode()) {
                        for (DataStream dataStream : multiPinWidget.dataStreams) {
                            if (dataStream.notEmptyAndIsValid()) {
                                expectedSyncCommandsCount++;
                            }
                        }
                    } else {
                        if (multiPinWidget.dataStreams[0].notEmptyAndIsValid()) {
                            expectedSyncCommandsCount++;
                        }
                    }
                }
            }
        }

        int dashId = profile.dashBoards[0].id;

        appClient.register(user, pass);
        appClient.login(user, pass, "Android", "1.10.4");
        int rand = ThreadLocalRandom.current().nextInt();
        appClient.send("addEnergy " + energy + "\0" + String.valueOf(rand));
        //we should wait until login finished. Only after that we can send commands
        verify(appClient.responseMock, timeout(1000)).channelRead(any(), eq(ok(2)));

        saveProfile(appClient, profile.dashBoards);

        appClient.activate(dashId);
        appClient.getToken(dashId);

        ArgumentCaptor<Object> objectArgumentCaptor = ArgumentCaptor.forClass(Object.class);
        verify(appClient.responseMock, timeout(2000).times(5 + profile.dashBoards.length + expectedSyncCommandsCount)).channelRead(any(), objectArgumentCaptor.capture());

        List<Object> arguments = objectArgumentCaptor.getAllValues();
        String token = getGetTokenMessage(arguments).body;

        hardClient.login(token);
        verify(hardClient.responseMock, timeout(2000)).channelRead(any(), eq(ok(1)));
        verify(appClient.responseMock, timeout(2000)).channelRead(any(), eq(hardwareConnected(1, "" + dashId + "-0")));

        appClient.reset();
        hardClient.reset();

        return new ClientPair(appClient, hardClient, token);
    }

}
