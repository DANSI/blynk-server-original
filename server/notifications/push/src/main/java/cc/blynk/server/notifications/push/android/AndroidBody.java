package cc.blynk.server.notifications.push.android;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 06.05.17.
 */
class AndroidBody {

    private final String message;
    private final int dashId;

    AndroidBody(String message, int dashId) {
        this.message = message;
        this.dashId = dashId;
    }

}
