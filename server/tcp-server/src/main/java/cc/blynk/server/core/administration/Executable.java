package cc.blynk.server.core.administration;

import cc.blynk.server.dao.SessionsHolder;
import cc.blynk.server.dao.UserRegistry;

import java.util.List;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 20.04.15.
 */
public interface Executable {

    public List<String> execute(UserRegistry userRegistry, SessionsHolder sessionsHolder, String... params);

}
