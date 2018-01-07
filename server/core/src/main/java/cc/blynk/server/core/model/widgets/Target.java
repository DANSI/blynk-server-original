package cc.blynk.server.core.model.widgets;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 05.01.17.
 */
public interface Target {

    //device ids that target should operate with
    int[] getDeviceIds();

    boolean contains(int deviceId);

    int[] getAssignedDeviceIds();

    int getDeviceId();

    default boolean isTag() {
        return false;
    }

}
