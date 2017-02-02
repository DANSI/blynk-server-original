package cc.blynk.server.core.model.widgets;

import cc.blynk.server.core.model.auth.Session;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 02.12.15.
 */
public interface FrequencyWidget {

    int READING_MSG_ID = 7778;

    void sendReadingCommand(Session session, int dashId);

    boolean isTicked(long now);

}
