package cc.blynk.server.handlers.hardware.http;

import cc.blynk.server.utils.JsonParser;
import io.netty.handler.codec.http.QueryStringDecoder;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 06.12.15.
 */
public class URIDecoder extends QueryStringDecoder {

    public final String[] paths;

    public URIDecoder(String uri) {
        super(uri);
        this.paths = path().split("/");
    }

    public String getHandlerType() {
        return paths[1].toLowerCase();
    }

    public String getEntity() {
        return paths[2].toLowerCase();
    }

    public String getSubEntity() {
        return paths[3].toLowerCase();
    }

    public boolean hasId() {
        return paths.length > 3;
    }

    public String getId() {
        return paths[3];
    }

    public int getPage() {
        if (parameters().get("_page") == null) {
            return 1;
        }
        return Integer.parseInt(parameters().get("_page").get(0));
    }

    public int getPageSize() {
        if (parameters().get("_perPage") == null) {
            return 1;
        }
        return Integer.parseInt(parameters().get("_perPage").get(0));
    }

    public String getSortField() {
        if (parameters().get("_sortField") == null) {
            return null;
        }
        return parameters().get("_sortField").get(0);
    }

    public String getSortOrder() {
        if (parameters().get("_sortDir") == null) {
            return null;
        }
        return parameters().get("_sortDir").get(0);
    }

    public String getNameFilter() {
        if (parameters().get("_filters") == null) {
            return null;
        }
        Filter filter = JsonParser.readAny(parameters().get("_filters").get(0), Filter.class);
        return  filter == null ? null : filter.name;
    }

}
