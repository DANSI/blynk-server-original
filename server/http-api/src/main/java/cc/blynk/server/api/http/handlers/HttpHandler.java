package cc.blynk.server.api.http.handlers;

import cc.blynk.server.core.BaseHttpHandler;
import cc.blynk.server.core.dao.SessionDao;
import cc.blynk.server.core.dao.UserDao;
import cc.blynk.server.handlers.http.logic.FileLogic;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpRequest;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 01.01.16.
 */
public class HttpHandler extends BaseHttpHandler {

    private static final String FAV_ICON_PATH = "/favicon.ico";

    private final FileLogic fileLogic;

    public HttpHandler(UserDao userDao, SessionDao sessionDao) {
        super(userDao, sessionDao);
        this.fileLogic = new FileLogic();
    }

    @Override
    public void processHttp(ChannelHandlerContext ctx, HttpRequest req) {
        //a bit ugly code but it is ok for now. 2 branches. 1 fro static files, second for normal http api
        switch (req.getUri()) {
            case FAV_ICON_PATH :
                req.setUri("/admin/static/favicon.ico");
                try {
                    fileLogic.channelRead(ctx, req);
                } catch (Exception e) {
                    log.error("Error handling static file.", e);
                }
                break;
            default:
                super.processHttp(ctx, req);
        }
    }

}
