package cc.blynk.server.handlers.http;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 01.12.15.
 */
public class PostUri {

    public final SimplePin pin;

    public PostUri(String[] paths) {
        this.pin = new SimplePin(paths[2], paths[3]);
    }
}
