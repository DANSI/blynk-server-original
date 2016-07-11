package cc.blynk.core.http.handlers;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 11.07.16.
 */
public class StaticFile {

    public final String path;

    public final boolean isDirectPath;

    public final boolean doCaching;

    public StaticFile(String path, boolean isDirectPath) {
        this.path = path;
        this.isDirectPath = isDirectPath;
        this.doCaching = true;
    }

    public StaticFile(String path, boolean isDirectPath, boolean doCaching) {
        this.path = path;
        this.isDirectPath = isDirectPath;
        this.doCaching = doCaching;
    }
}
