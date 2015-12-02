package cc.blynk.server.model.widgets.outputs;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 02.12.15.
 */
public interface FrequencyWidget {

    int getFrequency();

    long getLastRequestTS();

    void setLastRequestTS(long now);

    default boolean isTicked() {
        final long now = System.currentTimeMillis();
        if (getFrequency() > 0 && now > getLastRequestTS() + getFrequency()) {
            setLastRequestTS(now);
            return true;
        }
        return false;
    }

}
