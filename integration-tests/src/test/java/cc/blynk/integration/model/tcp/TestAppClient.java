package cc.blynk.integration.model.tcp;

import cc.blynk.client.core.AppClient;
import cc.blynk.client.handlers.decoders.ClientMessageDecoder;
import cc.blynk.integration.model.SimpleClientHandler;
import cc.blynk.server.core.protocol.handlers.encoders.MessageEncoder;
import cc.blynk.server.core.protocol.model.messages.StringMessage;
import cc.blynk.server.core.stats.GlobalStats;
import cc.blynk.utils.ServerProperties;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.util.List;
import java.util.Random;

import static org.mockito.Mockito.*;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 1/31/2015.
 */
public class TestAppClient extends AppClient {

    public final SimpleClientHandler responseMock = Mockito.mock(SimpleClientHandler.class);
    protected int msgId = 0;

    public TestAppClient(String host, int port) {
        super(host, port, Mockito.mock(Random.class), props);
        Mockito.when(random.nextInt(Short.MAX_VALUE)).thenReturn(1);
    }

    public TestAppClient(String host, int port, ServerProperties properties) {
        this(host, port, properties, new NioEventLoopGroup());
    }

    public TestAppClient(String host, int port, ServerProperties properties, NioEventLoopGroup nioEventLoopGroup) {
        super(host, port, Mockito.mock(Random.class), properties);
        Mockito.when(random.nextInt(Short.MAX_VALUE)).thenReturn(1);
        this.nioEventLoopGroup = nioEventLoopGroup;
    }

    public String getBody() throws Exception {
        ArgumentCaptor<StringMessage> objectArgumentCaptor = ArgumentCaptor.forClass(StringMessage.class);
        verify(responseMock, timeout(1000)).channelRead(any(), objectArgumentCaptor.capture());
        List<StringMessage> arguments = objectArgumentCaptor.getAllValues();
        StringMessage getTokenMessage = arguments.get(0);
        return getTokenMessage.body;
    }

    public void start() {
        Bootstrap b = new Bootstrap();
        b.group(nioEventLoopGroup).channel(NioSocketChannel.class).handler(getChannelInitializer());

        try {
            // Start the connection attempt.
            this.channel = b.connect(host, port).sync().channel();
        } catch (InterruptedException e) {
            log.error(e);
        }
    }

    @Override
    public ChannelInitializer<SocketChannel> getChannelInitializer() {
        return new ChannelInitializer<SocketChannel>() {
            @Override
            protected void initChannel(SocketChannel ch) throws Exception {
                ch.pipeline().addLast(
                        sslCtx.newHandler(ch.alloc(), host, port),
                        new ClientMessageDecoder(),
                        new MessageEncoder(new GlobalStats()),
                        responseMock
                );
            }
        };
    }

    public TestAppClient send(String line) {
        send(produceMessageBaseOnUserInput(line, ++msgId));
        return this;
    }

    public TestAppClient send(String line, int id) {
        send(produceMessageBaseOnUserInput(line, id));
        return this;
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
