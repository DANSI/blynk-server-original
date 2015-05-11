package cc.blynk.integration.model;

import cc.blynk.common.handlers.common.decoders.MessageDecoder;
import cc.blynk.common.handlers.common.encoders.MessageEncoder;
import cc.blynk.common.utils.ServerProperties;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.ssl.IdentityCipherSuiteFilter;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SslProvider;
import org.mockito.Mock;
import org.mockito.Mockito;

import javax.net.ssl.SSLException;
import java.io.BufferedReader;
import java.io.File;
import java.util.Random;

/**
 * The Blynk Project.
 * Created by Andrew Zakordonets.
 * Created on 03/5/2015.
 */
public class TestMutualAppClient extends TestAppClient {

    @Mock
    public final SimpleClientHandler responseMock = Mockito.mock(SimpleClientHandler.class);

    private SslContext sslCtx;
    @Mock
    Random random;
    private int msgId = 0;
    private ChannelPipeline pipeline;

    public TestMutualAppClient(String host, int port, ServerProperties props) {
        super(host, port, props);
        log.info("Creating app client. Host {}, sslPort : {}", host, port);
        try {
            this.sslCtx = SslContextBuilder.forClient()
                    .sslProvider(SslProvider.JDK)
                    .trustManager(new File(props.getProperty("server.ssl.cert")))
                    .keyManager(new File(props.getProperty("client.ssl.cert")),
                                new File(props.getProperty("client.ssl.key")),
                                props.getProperty("server.ssl.key.pass"))
                    .ciphers(null, IdentityCipherSuiteFilter.INSTANCE)
                    .sessionCacheSize(0)
                    .sessionTimeout(0)
                    .build();
        } catch (SSLException e) {

            log.error("Error initializing SSL context. Reason : {}", e.getMessage());
            log.debug(e);
            throw new RuntimeException();
        }
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
                ChannelPipeline pipeline = ch.pipeline();
                TestMutualAppClient.this.pipeline = pipeline;
                pipeline.addLast(sslCtx.newHandler(ch.alloc(), host, port));
                pipeline.addLast(new MessageDecoder());
                pipeline.addLast(new MessageEncoder());
                pipeline.addLast(responseMock);
            }
        };
    }

    public TestMutualAppClient send(String line) {
        send(produceMessageBaseOnUserInput(line, ++msgId));
        return this;
    }

    public TestMutualAppClient send(String line, int id) {
        send(produceMessageBaseOnUserInput(line, id));
        return this;
    }

    public void reset() {
        Mockito.reset(responseMock);
        msgId = 0;
    }

}
