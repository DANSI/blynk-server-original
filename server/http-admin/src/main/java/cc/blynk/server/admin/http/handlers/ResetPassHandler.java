package cc.blynk.server.admin.http.handlers;

import cc.blynk.server.core.BaseHttpHandler;
import cc.blynk.server.core.dao.SessionDao;
import cc.blynk.server.core.dao.UserDao;
import cc.blynk.server.core.stats.GlobalStats;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 01.12.15.
 */
public class ResetPassHandler extends BaseHttpHandler {

    public ResetPassHandler(UserDao userDao, SessionDao sessionDao, GlobalStats globalStats) {
        super(userDao, sessionDao, globalStats);
    }

}
