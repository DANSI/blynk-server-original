package cc.blynk.server.notifications;

import cc.blynk.server.utils.JsonParser;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 26.06.15.
 */
public class AndroidGCMMessage implements GCMMessage {

    private final String to;

    private final GCMData data;

    public AndroidGCMMessage(String to, String message, int dashId) {
        this.to = to;
        this.data = new GCMData(message, dashId);
    }

    @Override
    public String getToken() {
        return to;
    }

    @Override
    public String toString() {
        return JsonParser.toJson(this);
    }

    private class GCMData {
        private final String message;
        private final int dashId;

        public GCMData(String message, int dashId) {
            this.message = message;
            this.dashId = dashId;
        }
    }
}
