package cc.blynk.server.admin.http.handlers;

import cc.blynk.server.core.BaseHttpHandler;
import cc.blynk.server.core.dao.SessionDao;
import cc.blynk.server.core.dao.UserDao;
import cc.blynk.server.core.stats.GlobalStats;
import cc.blynk.server.handlers.http.logic.FileLogic;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpRequest;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 01.12.15.
 */
public class ResetPassHandler extends BaseHttpHandler {

    private final FileLogic fileHandler;

    public ResetPassHandler(UserDao userDao, SessionDao sessionDao, GlobalStats globalStats) {
        super(userDao, sessionDao, globalStats);
        this.fileHandler = new FileLogic();
    }

    @Override
    public void processHttp(ChannelHandlerContext ctx, HttpRequest req) {
        if (req.getUri().equals("/favicon.ico")) {
            req.setUri("/admin/static/favicon.ico");
            try {
                fileHandler.channelRead(ctx, req);
            } catch (Exception e) {
                log.error("Error handling static file.", e);
            }
        } else {
            super.processHttp(ctx, req);
        }
    }

}
