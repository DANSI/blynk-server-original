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
public class AdminHandler extends BaseHttpHandler {

    private final FileLogic fileHandler;
    private final String adminRootPath;
    private final String businessRootPath;

    public AdminHandler(UserDao userDao, SessionDao sessionDao, GlobalStats globalStats, String adminRootPath, String businessRootPath, boolean isUnpacked) {
        super(userDao, sessionDao, globalStats);
        this.adminRootPath = adminRootPath;
        this.businessRootPath = businessRootPath;
        this.fileHandler = new FileLogic(isUnpacked);
    }

    @Override
    public void processHttp(ChannelHandlerContext ctx, HttpRequest req) {
        //a bit ugly code but it is ok for now. 2 branches. 1 fro static files, second for normal http api
        if (req.getUri().equals(adminRootPath)) {
            req.setUri("/admin/static/admin.html");
        } else if (req.getUri().equals(businessRootPath)) {
            req.setUri("/admin/static/business.html");
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
            super.processHttp(ctx, req);
        }
    }

}
