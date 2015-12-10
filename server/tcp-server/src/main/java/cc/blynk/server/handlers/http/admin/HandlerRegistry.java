package cc.blynk.server.handlers.http.admin;

import cc.blynk.server.handlers.http.HttpResponse;
import cc.blynk.server.handlers.http.URIDecoder;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpRequest;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.glassfish.jersey.uri.UriTemplate;

import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.netty.handler.codec.http.HttpResponseStatus.*;
import static io.netty.handler.codec.http.HttpVersion.*;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 09.12.15.
 */
public class HandlerRegistry {

    private static final Logger log = LogManager.getLogger(HandlerRegistry.class);

    private final static List<HandlerHolder> processors = new ArrayList<>();

    public static void register(Object o) {
        registerHandler(o);
    }

    private static void registerHandler(Object handler) {
        Class<?> handlerClass = handler.getClass();
        Annotation pathAnnotation = handlerClass.getAnnotation(Path.class);
        String handlerMainPath = ((Path) pathAnnotation).value();

        for (Method method : handlerClass.getMethods()) {
            Annotation path = method.getAnnotation(Path.class);
            if (path != null) {
                String fullPath = handlerMainPath + ((Path) path).value();
                UriTemplate uriTemplate = new UriTemplate(fullPath);

                HandlerHolder handlerHolder = new HandlerHolder(uriTemplate, method, handler, method.getParameterCount());

                for (int i = 0; i < method.getParameterCount(); i++) {
                    Parameter parameter = method.getParameters()[i];

                    Annotation queryParamAnnotation = parameter.getAnnotation(QueryParam.class);
                    if (queryParamAnnotation != null) {
                        handlerHolder.params[i] = new QueyMethodParam(((QueryParam) queryParamAnnotation).value(), parameter.getType());
                    }

                    Annotation pathParamAnnotation = parameter.getAnnotation(PathParam.class);
                    if (pathParamAnnotation != null) {
                        handlerHolder.params[i] = new PathMethodParam(((PathParam) pathParamAnnotation).value(), parameter.getType());
                    }

                    if (pathParamAnnotation == null && queryParamAnnotation == null) {
                        throw new RuntimeException("Handler methods with parameters without @PathParam or @QueryParam not supported.");
                    }
                }

                processors.add(handlerHolder);
            }
        }
    }

    public static FullHttpResponse process(HttpRequest req) {
        URIDecoder uriDecoder = new URIDecoder(req.getUri());
        Map<String, String> pathData = new HashMap<>();
        for (HandlerHolder handlerHolder : processors) {
            if (handlerHolder.httpMethod == req.getMethod() &&
                    handlerHolder.uriTemplate.match(uriDecoder.path(), pathData)) {
                uriDecoder.pathData = pathData;
                try {
                    return (FullHttpResponse) handlerHolder.method.invoke(handlerHolder.handler, handlerHolder.fetchParams(uriDecoder));
                } catch (Exception e) {
                    System.out.println(e);
                }

            }
        }

        log.error("Error resolving url. No path found.");
        return new HttpResponse(HTTP_1_1, NOT_FOUND);
    }

}
