package cc.blynk.server.core.administration;

import cc.blynk.server.dao.SessionsHolder;
import cc.blynk.server.dao.UserRegistry;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 20.04.15.
 */
public class ExecutorImplementation implements Executable {

    @Override
    public String execute(UserRegistry userRegistry, SessionsHolder sessionsHolder) {
        return "Test success!";
    }
}
