package cc.blynk.core.http.handlers;

import cc.blynk.server.core.dao.SessionDao;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.FullHttpRequest;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 13.05.16.
 */
@ChannelHandler.Sharable
public class CookieBasedUrlReWriterHandler extends ChannelInboundHandlerAdapter {

    private final String initUrl;
    private final String mapToUrlWithCookie;
    private final String mapToUrlWithoutCookie;

    public CookieBasedUrlReWriterHandler(String initUrl, String mapToUrlWithCookie, String mapToUrlWithoutCookie) {
        this.initUrl = initUrl;
        this.mapToUrlWithCookie = mapToUrlWithCookie;
        this.mapToUrlWithoutCookie = mapToUrlWithoutCookie;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof FullHttpRequest) {
            FullHttpRequest request = (FullHttpRequest) msg;
            if (request.uri().equals(initUrl)) {
                if (ctx.channel().attr(SessionDao.userAttributeKey).get() == null) {
                    request.setUri(mapToUrlWithoutCookie);
                } else {
                    request.setUri(mapToUrlWithCookie);
                }
            }
        }

        super.channelRead(ctx, msg);
    }

}
