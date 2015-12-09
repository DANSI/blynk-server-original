package cc.blynk.server.handlers.http;

import cc.blynk.server.model.auth.User;
import cc.blynk.server.utils.JsonParser;

import java.util.Collection;

import static cc.blynk.server.utils.ListUtils.*;
import static io.netty.handler.codec.http.HttpResponseStatus.*;
import static io.netty.handler.codec.http.HttpVersion.*;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 06.12.15.
 */
public class ResponseGenerator {

    private static final String JSON = "application/json";
    private static final String PLAIN_TEXT = "text/plain";

    public static HttpResponse makeResponse(String data) {
        return new HttpResponse(HTTP_1_1, OK, data, JSON);
    }

    public static HttpResponse makeResponse(User user) {
        return makeResponse(JsonParser.toJson(user));
    }

    public static HttpResponse makeResponse(Collection<?> list, int page, int size) {
        return makeResponse(JsonParser.toJson(subList(list, page, size)));
    }

    public static HttpResponse makeResponse(Collection<?> list) {
        return makeResponse(JsonParser.toJson(list));
    }

    public static HttpResponse appendTotalCountHeader(HttpResponse httpResponse, int count) {
        httpResponse.headers().set("X-Total-Count", count);
        httpResponse.headers().set("Access-Control-Expose-Headers", "x-total-count");
        return httpResponse;
    }

}
