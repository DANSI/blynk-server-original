package cc.blynk.server.handlers.hardware.http;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 01.12.15.
 */
public class GetUri {

    public final SimplePin pin;

    public GetUri(String[] paths) {
        this.pin = new SimplePin(paths[2]);
    }
}
