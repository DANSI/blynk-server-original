package cc.blynk.utils.http;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 10.01.18.
 */
public enum ContentType {

    TEXT_HTML(MediaType.TEXT_HTML),
    TEXT_PLAIN(MediaType.TEXT_PLAIN);

    public final String label;

    ContentType(String label) {
        this.label = label;
    }

}
