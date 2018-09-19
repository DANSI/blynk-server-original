package cc.blynk.integration.model.tcp;

import cc.blynk.client.core.AppClient;
import cc.blynk.integration.TestUtil;
import cc.blynk.integration.model.SimpleClientHandler;
import cc.blynk.utils.properties.ServerProperties;
import org.mockito.Mockito;

import java.util.Random;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.after;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 05.02.18.
 */
public abstract class BaseTestAppClient extends AppClient {

    public final SimpleClientHandler responseMock = Mockito.mock(SimpleClientHandler.class);
    int msgId = 0;

    public BaseTestAppClient(String host, int port, Random msgIdGenerator, ServerProperties properties) {
        super(host, port, msgIdGenerator, properties);
    }

    public void verifyResult(Object exceptedResult) throws Exception {
        verifyResult(exceptedResult, 1);
    }

    public void never(Object exceptedResult) throws Exception {
        verify(responseMock, Mockito.never()).channelRead(any(), eq(exceptedResult));
    }

    public void neverAfter(int time, Object exceptedResult) throws Exception {
        verify(responseMock, after(time).never()).channelRead(any(), eq(exceptedResult));
    }

    public void verifyResult(Object exceptedResult, int times) throws Exception {
        verify(responseMock, timeout(1000).times(times)).channelRead(any(), eq(exceptedResult));
    }

    public void verifyResultAfter(int time, Object exceptedResult) throws Exception {
        verify(responseMock, after(time)).channelRead(any(), eq(exceptedResult));
    }

    public void verifyAny() throws Exception {
        verify(responseMock, timeout(500)).channelRead(any(), any());
    }

    public String getBody() throws Exception {
        return TestUtil.getBody(responseMock, 1);
    }

    public String getBody(int expectedMessageOrder) throws Exception {
        return TestUtil.getBody(responseMock, expectedMessageOrder);
    }

    public void reset() {
        Mockito.reset(responseMock);
        msgId = 0;
    }

    public void send(String line) {
        send(produceMessageBaseOnUserInput(line, ++msgId));
    }
}
