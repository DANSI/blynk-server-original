package cc.blynk.integration.tcp;

import cc.blynk.integration.IntegrationBase;
import cc.blynk.integration.model.MockHolder;
import cc.blynk.integration.model.tcp.TestHardClient;
import cc.blynk.server.core.BaseServer;
import cc.blynk.server.core.protocol.model.messages.ResponseMessage;
import cc.blynk.server.hardware.HardwareServer;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.OngoingStubbing;

import java.io.BufferedReader;

import static cc.blynk.server.core.protocol.enums.Response.*;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 2/2/2015.
 *
 * Basic integration test. Allows to test base commands workflow. Thus netty is asynchronous
 * test is little bit complex, but I don't know right now how to make it better and simpler.
 *
 */
@RunWith(MockitoJUnitRunner.class)
public class HardProtocolCommandsTest extends IntegrationBase {

    @Mock
    public BufferedReader bufferedReader;
    private BaseServer hardwareServer;

    @Before
    public void init() throws Exception {
        this.hardwareServer = new HardwareServer(holder).start(transportTypeHolder);
    }

    @After
    public void shutdown() {
        this.hardwareServer.stop();
    }

    @Test
    public void testInvalidHardwareTokenException() throws Exception {
        makeCommands("login 123").check(new ResponseMessage(1, INVALID_TOKEN));
    }

    @Test
    public void testInvalidCommandAppLoginOnHardChannel() throws Exception {
        makeCommands("login dima@dima.ua 1").check(new ResponseMessage(1, INVALID_TOKEN));

        String body = b("dima@dima.ua 1");
        makeCommands("login " + body).check(new ResponseMessage(1, INVALID_TOKEN));

        makeCommands("login").check(new ResponseMessage(1, INVALID_TOKEN));
    }

    @Test
    public void testNoRegisterHandlerNoResponse() throws Exception {
        TestHardClient hardClient = new TestHardClient("localhost", tcpHardPort);
        OngoingStubbing<String> ongoingStubbing = when(bufferedReader.readLine()).thenReturn("register dima@dima.ua 1");
        ongoingStubbing.thenAnswer(invocation -> {
            //todo think how to avoid this
            sleep(400);
            return "quit";
        });
        hardClient.start(bufferedReader);

        verify(hardClient.responseMock, never()).channelRead(any(), any());
    }

    @Test
    @Ignore
    //todo finish and fix.
    public void testInvalidTweetBody() throws Exception {
        makeCommands("register dmitriy@mail.ua 1").check(OK);

        makeCommands("login dmitriy@mail.ua 1", "tweet").check(OK).check(new ResponseMessage(1, NOTIFICATION_INVALID_BODY_EXCEPTION));
    }

    /**
     * 1) Creates client socket;
     * 2) Sends commands;
     * 3) Sleeps for 100ms between every command send;
     * 4) Checks that sever response is OK;
     * 5) Closing socket.
     */
    private MockHolder makeCommands(String... commands) throws Exception {
        TestHardClient hardClient = new TestHardClient("localhost", tcpHardPort);

        OngoingStubbing<String> ongoingStubbing = when(bufferedReader.readLine());
        for (String cmd : commands) {
            ongoingStubbing = ongoingStubbing.thenReturn(cmd);
        }

        ongoingStubbing.thenAnswer(invocation -> {
            //todo think how to avoid this
            sleep(400);
            return "quit";
        });

        hardClient.start(bufferedReader);

        verify(hardClient.responseMock, times(commands.length)).channelRead(any(), any());
        return new MockHolder(hardClient.responseMock);
    }

}
