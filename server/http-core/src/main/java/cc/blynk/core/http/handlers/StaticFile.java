package cc.blynk.core.http.handlers;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 11.07.16.
 */
public class StaticFile {

    public final String path;

    public StaticFile(String path) {
        this.path = path;
    }

    public boolean isStatic(String url) {
        return url.startsWith(path);
    }
}
