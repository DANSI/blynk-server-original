package cc.blynk.server.websocket.handlers;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.websocketx.WebSocketHandshakeException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 09.08.16.
 */
@ChannelHandler.Sharable
public class ExceptionCatcherHandler extends ChannelInboundHandlerAdapter {

    private static final Logger log = LogManager.getLogger(ExceptionCatcherHandler.class);

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        if (cause instanceof WebSocketHandshakeException) {
            log.debug("Web Socket Handshake Exception.", cause);
        }
    }

}
