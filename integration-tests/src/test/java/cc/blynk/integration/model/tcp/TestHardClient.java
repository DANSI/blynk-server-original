package cc.blynk.integration.model.tcp;

import cc.blynk.client.core.BaseClient;
import cc.blynk.client.handlers.decoders.ClientMessageDecoder;
import cc.blynk.integration.model.SimpleClientHandler;
import cc.blynk.server.core.protocol.handlers.encoders.MessageEncoder;
import cc.blynk.server.core.stats.GlobalStats;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import org.mockito.Mockito;

import java.util.Random;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 1/31/2015.
 */
public class TestHardClient extends BaseClient {

    public final SimpleClientHandler responseMock;

    private int msgId;

    public TestHardClient(String host, int port) {
        this(host, port, new NioEventLoopGroup());
    }

    public TestHardClient(String host, int port, NioEventLoopGroup nioEventLoopGroup) {
        super(host, port, Mockito.mock(Random.class));
        Mockito.when(random.nextInt(Short.MAX_VALUE)).thenReturn(1);
        this.nioEventLoopGroup = nioEventLoopGroup;

        this.responseMock = Mockito.mock(SimpleClientHandler.class);
        this.msgId = 0;
    }

    @Override
    public ChannelInitializer<SocketChannel> getChannelInitializer() {
        return new ChannelInitializer<SocketChannel>() {
            @Override
            protected void initChannel(SocketChannel ch) throws Exception {
                ch.pipeline().addLast(
                        new ClientMessageDecoder(),
                        new MessageEncoder(new GlobalStats()),
                        responseMock
                );
            }
        };
    }

    public TestHardClient send(String line) {
        send(produceMessageBaseOnUserInput(line, ++msgId));
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
