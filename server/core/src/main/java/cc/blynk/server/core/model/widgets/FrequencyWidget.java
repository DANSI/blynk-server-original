package cc.blynk.server.core.model.widgets;

import cc.blynk.server.core.model.auth.Session;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 02.12.15.
 */
public interface FrequencyWidget {

    int READING_MSG_ID = 7778;

    int getFrequency();

    long getLastRequestTS();

    void setLastRequestTS(long now);

    void sendReadingCommand(Session session, int dashId);

    default boolean isTicked(long now) {
        final int frequency = getFrequency();
        if (frequency > 0 && now > getLastRequestTS() + frequency) {
            setLastRequestTS(now);
            return true;
        }
        return false;
    }

}
