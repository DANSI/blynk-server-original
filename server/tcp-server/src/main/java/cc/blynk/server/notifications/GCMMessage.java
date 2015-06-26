package cc.blynk.server.notifications;

import cc.blynk.server.utils.JsonParser;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 26.06.15.
 */
public class GCMMessage {

    String to;

    GCMData data;

    public GCMMessage(String to, String message) {
        this.to = to;
        this.data = new GCMData(message);
    }

    @Override
    public String toString() {
        return JsonParser.toJson(this);
    }

    private class GCMData {
        String message;

        public GCMData(String message) {
            this.message = message;
        }
    }
}
