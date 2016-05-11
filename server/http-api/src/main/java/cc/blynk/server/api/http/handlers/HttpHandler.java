package cc.blynk.server.api.http.handlers;

import cc.blynk.server.core.BaseHttpHandler;
import cc.blynk.server.core.dao.SessionDao;
import cc.blynk.server.core.dao.UserDao;
import cc.blynk.server.core.stats.GlobalStats;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 01.01.16.
 */
public class HttpHandler extends BaseHttpHandler {

    public HttpHandler(UserDao userDao, SessionDao sessionDao, GlobalStats globalStats) {
        super(userDao, sessionDao, globalStats);
    }

}
