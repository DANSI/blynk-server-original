package cc.blynk.server.core.model.widgets.outputs;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 02.12.15.
 */
public interface FrequencyWidget {

    int getFrequency();

    long getLastRequestTS(String body);

    void setLastRequestTS(String body, long now);

    default boolean isTicked(String body) {
        final long now = System.currentTimeMillis();
        if (getFrequency() > 0 && now > getLastRequestTS(body) + getFrequency()) {
            setLastRequestTS(body, now);
            return true;
        }
        return false;
    }

}
