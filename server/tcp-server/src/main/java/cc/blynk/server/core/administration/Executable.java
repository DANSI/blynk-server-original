package cc.blynk.server.core.administration;

import cc.blynk.server.dao.SessionDao;
import cc.blynk.server.dao.UserDao;

import java.util.List;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 20.04.15.
 */
public interface Executable {

    List<String> execute(UserDao userDao, SessionDao sessionDao, String... params);

}
