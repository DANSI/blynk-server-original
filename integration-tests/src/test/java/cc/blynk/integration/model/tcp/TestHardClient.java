package cc.blynk.integration.model.tcp;

import cc.blynk.client.handlers.decoders.ClientMessageDecoder;
import cc.blynk.integration.model.SimpleClientHandler;
import cc.blynk.server.core.model.enums.PinType;
import cc.blynk.server.core.protocol.handlers.encoders.MessageEncoder;
import cc.blynk.server.core.stats.GlobalStats;
import cc.blynk.utils.StringUtils;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import org.mockito.Mockito;

import java.util.Random;

import static cc.blynk.utils.StringUtils.BODY_SEPARATOR;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 1/31/2015.
 */
public class TestHardClient extends BaseTestHardwareClient {

    public TestHardClient(String host, int port) {
        this(host, port, new NioEventLoopGroup());
    }

    public TestHardClient(String host, int port, NioEventLoopGroup nioEventLoopGroup) {
        super(host, port, Mockito.mock(Random.class));
        this.nioEventLoopGroup = nioEventLoopGroup;
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

    public void login(String token) {
        send("hardwareLogin " + token);
    }

    public void setProperty(int pin, String property, String value) {
        send("setProperty " + pin + " " + property + " " + value);
    }

    public void setProperty(int pin, String property, String... value) {
        send("setProperty " + pin + " " + property + " " + String.join(StringUtils.BODY_SEPARATOR_STRING, value));
    }

    public void sync() {
        send("hardsync");
    }

    public void sync(PinType pinType, int pin) {
        send("hardsync " + pinType.pintTypeChar + "r" + BODY_SEPARATOR + pin);
    }

    public void sync(PinType pinType, int pin1, int pin2) {
        send("hardsync " + pinType.pintTypeChar + "r" + BODY_SEPARATOR + pin1 + BODY_SEPARATOR + pin2);
    }

    public void replace(SimpleClientHandler simpleClientHandler) {
        this.channel.pipeline().removeLast();
        this.channel.pipeline().addLast(simpleClientHandler);
    }

}
