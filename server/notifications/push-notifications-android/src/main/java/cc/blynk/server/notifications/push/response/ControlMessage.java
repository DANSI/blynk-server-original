package cc.blynk.server.notifications.push.response;

import cc.blynk.server.notifications.push.response.enums.ControlType;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 2/8/2015.
 */
public class ControlMessage extends ResponseMessageBase {

    public ControlType control_type;

    @Override
    public String toString() {
        return "ControlMessage{" +
                "control_type='" + control_type + "'," +
                super.toString() + "} ";
    }
}
