package cc.blynk.server.handlers.http;

import io.netty.handler.codec.http.QueryStringDecoder;

import java.util.Map;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 06.12.15.
 */
public class URIDecoder extends QueryStringDecoder {

    public final String[] paths;
    public Map<String, String> pathData;

    public URIDecoder(String uri) {
        super(uri);
        this.paths = path().split("/");
    }

}
