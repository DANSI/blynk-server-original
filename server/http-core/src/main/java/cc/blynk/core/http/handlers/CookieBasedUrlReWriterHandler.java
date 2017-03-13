package cc.blynk.core.http.handlers;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.FullHttpRequest;

import static io.netty.handler.codec.http.HttpHeaderNames.COOKIE;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 13.05.16.
 */
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
                if (request.headers().contains(COOKIE)) {
                    request.setUri(mapToUrlWithCookie);
                } else {
                    request.setUri(mapToUrlWithoutCookie);
                }
            }
        }

        super.channelRead(ctx, msg);
    }

}
