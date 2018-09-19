package cc.blynk.core.http.handlers.url;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 31.03.17.
 */
public class UrlMapper {

    public final String from;
    public final String to;

    public UrlMapper(String from, String to) {
        this.from = from;
        this.to = to;
    }

    public boolean isMatch(String uri) {
        return from.equals(uri);
    }
}
