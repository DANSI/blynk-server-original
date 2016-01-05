package cc.blynk.integration.model;


import cc.blynk.server.core.protocol.model.messages.MessageBase;
import cc.blynk.server.core.protocol.model.messages.ResponseMessage;

import static org.mockito.Mockito.*;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 3/1/2015.
 */
public class MockHolder {

    private SimpleClientHandler mock;

    public MockHolder(SimpleClientHandler mock) {
        this.mock = mock;
    }

    public MockHolder check(int responseMessageCode) throws Exception {
        verify(mock).channelRead(any(), eq(new ResponseMessage(1, responseMessageCode)));
        return this;
    }

    public MockHolder check(int times, int responseMessageCode) throws Exception {
        verify(mock, times(times)).channelRead(any(), eq(new ResponseMessage(1, responseMessageCode)));
        return this;
    }

    public MockHolder check(MessageBase responseMessage) throws Exception {
        verify(mock).channelRead(any(), eq(responseMessage));
        return this;
    }

    public MockHolder check(int times, MessageBase responseMessage) throws Exception {
        verify(mock, times(times)).channelRead(any(), eq(responseMessage));
        return this;
    }

}


