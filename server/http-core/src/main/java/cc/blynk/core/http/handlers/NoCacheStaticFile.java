package cc.blynk.core.http.handlers;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 11.07.16.
 */
public class NoCacheStaticFile extends StaticFile {

    public NoCacheStaticFile(String path) {
        super(path);
    }
}
