package cc.blynk.core.http.handlers;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 11.07.16.
 */
public class StaticFile {

    public String path;

    public boolean isDirectPath;

    public StaticFile(String path, boolean isDirectPath) {
        this.path = path;
        this.isDirectPath = isDirectPath;
    }
}
