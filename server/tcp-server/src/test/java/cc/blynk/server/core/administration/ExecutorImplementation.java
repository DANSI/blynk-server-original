package cc.blynk.server.core.administration;

import cc.blynk.server.dao.SessionsHolder;
import cc.blynk.server.dao.UserRegistry;

import java.util.ArrayList;
import java.util.List;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 20.04.15.
 */
public class ExecutorImplementation implements Executable {

    @Override
    public List<String> execute(UserRegistry userRegistry, SessionsHolder sessionsHolder, String... params) {
        return new ArrayList<String>() {
            {
                add("Test success!");
            }
        };
    }
}
