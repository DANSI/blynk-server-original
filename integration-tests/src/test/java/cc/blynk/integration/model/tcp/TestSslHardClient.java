package cc.blynk.integration.model.tcp;

import cc.blynk.client.handlers.decoders.ClientMessageDecoder;
import cc.blynk.integration.model.SimpleClientHandler;
import cc.blynk.server.core.model.enums.PinType;
import cc.blynk.server.core.protocol.handlers.encoders.MessageEncoder;
import cc.blynk.server.core.stats.GlobalStats;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SslProvider;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import org.mockito.Mockito;

import javax.net.ssl.SSLException;
import java.io.File;
import java.util.Random;

import static cc.blynk.utils.StringUtils.BODY_SEPARATOR;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 1/31/2015.
 */
public class TestSslHardClient extends BaseTestHardwareClient {

    protected SslContext sslCtx;

    public TestSslHardClient(String host, int port) {
        this(host, port, new NioEventLoopGroup());
    }

    private TestSslHardClient(String host, int port, NioEventLoopGroup nioEventLoopGroup) {
        super(host, port, Mockito.mock(Random.class));
        this.nioEventLoopGroup = nioEventLoopGroup;
        log.info("Creating app client. Host {}, sslPort : {}", host, port);
        File serverCert = makeCertificateFile("server.ssl.cert");
        File clientCert = makeCertificateFile("client.ssl.cert");
        File clientKey = makeCertificateFile("client.ssl.key");
        try {
            if (!serverCert.exists() || !clientCert.exists() || !clientKey.exists()) {
                log.info("Enabling one-way auth with no certs checks.");
                this.sslCtx = SslContextBuilder.forClient().sslProvider(SslProvider.JDK)
                        .trustManager(InsecureTrustManagerFactory.INSTANCE)
                        .build();
            } else {
                log.info("Enabling mutual auth.");
                String clientPass = props.getProperty("client.ssl.key.pass");
                this.sslCtx = SslContextBuilder.forClient()
                        .sslProvider(SslProvider.JDK)
                        .trustManager(serverCert)
                        .keyManager(clientCert, clientKey, clientPass)
                        .build();
            }
        } catch (SSLException e) {
            log.error("Error initializing SSL context. Reason : {}", e.getMessage());
            log.debug(e);
            throw new RuntimeException(e);
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

    public void login(String token) {
        send("hardwareLogin " + token);
    }

    public void setProperty(int pin, String property, String value) {
        send("setProperty " + pin + " " + property + " " + value);
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
