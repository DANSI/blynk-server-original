package cc.blynk.server.handlers.http.helpers;

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

    public static Response makeResponse(String data) {
        return new Response(HTTP_1_1, OK, data, JSON);
    }

    public static Response makeResponse(User user) {
        return makeResponse(JsonParser.toJson(user));
    }

    public static Response makeResponse(Collection<?> list, int page, int size) {
        return makeResponse(JsonParser.toJson(subList(list, page, size)));
    }

    public static Response makeResponse(Collection<?> list) {
        return makeResponse(JsonParser.toJson(list));
    }

    public static Response appendTotalCountHeader(Response response, int count) {
        response.headers().set("X-Total-Count", count);
        response.headers().set("Access-Control-Expose-Headers", "x-total-count");
        return response;
    }

}
