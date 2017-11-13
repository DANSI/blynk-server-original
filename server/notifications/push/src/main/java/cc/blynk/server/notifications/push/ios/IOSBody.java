package cc.blynk.server.notifications.push.ios;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 14.11.17.
 */
class IOSBody {

    private final String body;
    private final int dashId;
    private final String sound;
    private String title;

    IOSBody(String body, int dashId) {
        this.body = body;
        this.dashId = dashId;
        this.sound = "default";
    }

    void setTitle(String title) {
        this.title = title;
    }
}
