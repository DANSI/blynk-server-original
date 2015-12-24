package cc.blynk.server.handlers.http.admin;

import cc.blynk.server.handlers.http.BaseHttpAPIHandler;
import cc.blynk.server.handlers.http.admin.handlers.FileHandler;
import cc.blynk.server.handlers.http.rest.HandlerRegistry;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpRequest;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 01.12.15.
 */
public class AdminHandler extends BaseHttpAPIHandler {

    private final FileHandler fileHandler = new FileHandler();

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        if (!(msg instanceof HttpRequest)) {
            return;
        }

        HttpRequest req = (HttpRequest) msg;

        log.info("{} : {}", req.getMethod().name(), req.getUri());

        //a bit ugly code but it is ok for now. 2 branches. 1 fro static files, second for normal http api
        if (req.getUri().equals("/admin")) {
            req.setUri("/admin/static/admin.html");
        }
        if (req.getUri().startsWith("/admin/static")) {
            try {
                fileHandler.channelRead(ctx, req);
            } catch (Exception e) {
                log.error("Error handling static file.", e);
            }
        } else {
            FullHttpResponse response = HandlerRegistry.process(req);
            send(ctx, req, response);
        }
    }

}
