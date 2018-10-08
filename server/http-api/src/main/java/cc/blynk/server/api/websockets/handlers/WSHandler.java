package cc.blynk.server.api.websockets.handlers;

import cc.blynk.server.core.protocol.enums.Command;
import cc.blynk.server.core.stats.GlobalStats;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketHandshakeException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 11.01.16.
 */
@ChannelHandler.Sharable
public class WSHandler extends ChannelInboundHandlerAdapter {

    private static final Logger log = LogManager.getLogger(WSHandler.class);

    private final GlobalStats globalStats;

    public WSHandler(GlobalStats globalStats) {
        this.globalStats = globalStats;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        globalStats.markWithoutGlobal(Command.WEB_SOCKETS);
        if (msg instanceof BinaryWebSocketFrame) {
            ctx.fireChannelRead(((BinaryWebSocketFrame) msg).content());
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        if (cause instanceof WebSocketHandshakeException) {
            log.debug("Web Socket Handshake Exception.", cause);
        }
    }

}
