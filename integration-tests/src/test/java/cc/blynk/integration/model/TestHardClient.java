package cc.blynk.integration.model;

import cc.blynk.client.core.HardwareClient;
import cc.blynk.client.handlers.decoders.ClientMessageDecoder;
import cc.blynk.server.core.protocol.handlers.encoders.MessageEncoder;
import cc.blynk.server.core.stats.GlobalStats;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import org.mockito.Mockito;

import java.io.BufferedReader;
import java.util.Random;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 1/31/2015.
 */
public class TestHardClient extends HardwareClient {

    public final SimpleClientHandler responseMock;

    private int msgId;


    public TestHardClient(String host, int port) {
        this(host, port, new NioEventLoopGroup());
    }

    public TestHardClient(String host, int port, NioEventLoopGroup nioEventLoopGroup) {
        super(host, port, Mockito.mock(Random.class));
        Mockito.when(random.nextInt(Short.MAX_VALUE)).thenReturn(1);

        this.responseMock = Mockito.mock(SimpleClientHandler.class);
        this.msgId = 0;
    }

    @Override
    public void start(BufferedReader commandInputStream) {
        if (commandInputStream == null) {
            nioEventLoopGroup = new NioEventLoopGroup();

            Bootstrap b = new Bootstrap();
            b.group(nioEventLoopGroup).channel(NioSocketChannel.class).handler(getChannelInitializer());

            try {
                // Start the connection attempt.
                this.channel = b.connect(host, port).sync().channel();
            } catch (InterruptedException e) {
                log.error(e);
            }
        } else {
            super.start(commandInputStream);
        }
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
