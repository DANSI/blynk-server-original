package cc.blynk.core.http.handlers.url;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.FullHttpRequest;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 13.05.16.
 */
@ChannelHandler.Sharable
public class UrlReWriterHandler extends ChannelInboundHandlerAdapter {

    private static final Logger log = LogManager.getLogger(UrlReWriterHandler.class);

    private final UrlMapper[] mappers;

    public UrlReWriterHandler(String from, String to) {
        this(new UrlMapper(from, to));
    }

    public UrlReWriterHandler(UrlMapper mapper) {
        this.mappers = new UrlMapper[] {mapper};
    }

    public UrlReWriterHandler(UrlMapper... mappers) {
        this.mappers = mappers;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof FullHttpRequest) {
            FullHttpRequest request = (FullHttpRequest) msg;

            String requestUri = request.uri();
            String mapToURI = mapTo(requestUri);
            log.trace("Mapping from {} to {}", requestUri, mapToURI);
            request.setUri(mapToURI);
        }

        super.channelRead(ctx, msg);
    }

    private String mapTo(String uri) {
        for (UrlMapper urlMapper : mappers) {
            if (urlMapper.isMatch(uri)) {
                return urlMapper.to;
            }
        }
        return uri;
    }

}
