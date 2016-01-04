package cc.blynk.server.admin.http.handlers;

import cc.blynk.server.handlers.http.BaseHttpAPIHandler;
import cc.blynk.server.handlers.http.logic.FileLogic;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpRequest;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 01.12.15.
 */
public class AdminHandler extends BaseHttpAPIHandler {

    private final FileLogic fileHandler;
    private final String rootPath;

    public AdminHandler(String rootPath) {
        this.rootPath = rootPath;
        this.fileHandler = new FileLogic();
    }

    @Override
    public void process(ChannelHandlerContext ctx, HttpRequest req) {
        //a bit ugly code but it is ok for now. 2 branches. 1 fro static files, second for normal http api
        if (req.getUri().equals(rootPath)) {
            req.setUri("/admin/static/admin.html");
        } else if (req.getUri().equals("/favicon.ico")) {
            req.setUri("/admin/static/favicon.ico");
        }
        if (req.getUri().startsWith("/admin/static")) {
            try {
                fileHandler.channelRead(ctx, req);
            } catch (Exception e) {
                log.error("Error handling static file.", e);
            }
        } else {
            super.process(ctx, req);
        }
    }

}
