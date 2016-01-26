/*
 * Copyright 2014 The Netty Project
 *
 * The Netty Project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package cc.blynk.integration.model.websocket;

import cc.blynk.client.core.BaseClient;
import cc.blynk.integration.model.SimpleClientHandler;
import cc.blynk.server.core.protocol.handlers.decoders.MessageDecoder;
import cc.blynk.server.core.protocol.model.messages.MessageBase;
import cc.blynk.server.core.stats.GlobalStats;
import cc.blynk.server.websocket.handlers.WebSocketHandler;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketClientHandshakerFactory;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketVersion;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import org.mockito.Mockito;

import java.net.URI;
import java.nio.ByteBuffer;
import java.util.Random;

public final class WebSocketClient extends BaseClient {

    public final SimpleClientHandler responseMock = Mockito.mock(SimpleClientHandler.class);
    final SslContext sslCtx;
    private final WebSocketClientHandler handler;
    protected int msgId = 0;

    public WebSocketClient(String host, int port, boolean isSSL) throws Exception {
        super(host, port, new Random());

        String scheme = isSSL ? "wss://" : "ws://";
        URI uri = new URI(scheme + host + ":" + port + WebSocketHandler.WEBSOCKET_PATH);

        if (isSSL) {
            sslCtx = SslContextBuilder.forClient().trustManager(InsecureTrustManagerFactory.INSTANCE).build();
        } else {
            sslCtx = null;
        }

        this.handler = new WebSocketClientHandler(
                        WebSocketClientHandshakerFactory.newHandshaker(
                                uri, WebSocketVersion.V13, null, false, new DefaultHttpHeaders()));
    }

    private static WebSocketFrame produceWebSocketFrame(MessageBase msg) {
        ByteBuffer bb = ByteBuffer.allocate(5 + msg.length);
        bb.put((byte) msg.command);
        bb.putShort((short) msg.id);
        bb.putShort((short) msg.length);
        byte[] data = msg.getBytes();
        if (data != null) {
            bb.put(data);
        }
        return new BinaryWebSocketFrame(Unpooled.wrappedBuffer(bb.array()));
    }

    @Override
    protected ChannelInitializer<SocketChannel> getChannelInitializer() {
        return new ChannelInitializer<SocketChannel> () {
            @Override
            public void initChannel(SocketChannel ch) throws Exception {
                ChannelPipeline p = ch.pipeline();
                if (sslCtx != null) {
                    p.addLast(sslCtx.newHandler(ch.alloc(), host, port));
                }
                p.addLast(
                        new HttpClientCodec(),
                        new HttpObjectAggregator(8192),
                        handler,
                        new MessageDecoder(new GlobalStats())
                );
            }
        };
    }

    @Override
    public void start() {
        super.start();
        startHandshake();
    }

    private void startHandshake() {
        handler.startHandshake(channel);
        try {
            handler.handshakeFuture().sync();
            this.channel.pipeline().addLast(responseMock);
        } catch (Exception e) {
            log.error(e);
        }
    }

    public WebSocketClient send(String line) {
        send(produceWebSocketFrame(produceMessageBaseOnUserInput(line, ++msgId)));
        return this;
    }
}
