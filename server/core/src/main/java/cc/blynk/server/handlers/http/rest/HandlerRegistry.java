package cc.blynk.server.handlers.http.rest;

import cc.blynk.utils.UriTemplate;
import io.netty.handler.codec.http.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.ws.rs.Consumes;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.HashSet;
import java.util.Set;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 09.12.15.
 */
public class HandlerRegistry {

    private static final Logger log = LogManager.getLogger(HandlerRegistry.class);

    private final static Set<HandlerHolder> processors = new HashSet<>();

    public static void register(String rootPath, Object o) {
        registerHandler(rootPath, o);
    }

    public static void register(Object o) {
        registerHandler("", o);
    }

    private static void registerHandler(String rootPath, Object handler) {
        Class<?> handlerClass = handler.getClass();
        Annotation pathAnnotation = handlerClass.getAnnotation(Path.class);
        String handlerMainPath = ((Path) pathAnnotation).value();

        for (Method method : handlerClass.getMethods()) {
            Annotation consumes = method.getAnnotation(Consumes.class);
            String contentType = MediaType.APPLICATION_JSON;
            if (consumes != null) {
                contentType = ((Consumes) consumes).value()[0];
            }

            Annotation path = method.getAnnotation(Path.class);
            if (path != null) {
                String fullPath = rootPath + handlerMainPath + ((Path) path).value();
                UriTemplate uriTemplate = new UriTemplate(fullPath);

                HandlerHolder handlerHolder = new HandlerHolder(uriTemplate, method, handler, method.getParameterCount());

                for (int i = 0; i < method.getParameterCount(); i++) {
                    Parameter parameter = method.getParameters()[i];

                    Annotation queryParamAnnotation = parameter.getAnnotation(javax.ws.rs.QueryParam.class);
                    if (queryParamAnnotation != null) {
                        handlerHolder.params[i] = new QueryParam(((javax.ws.rs.QueryParam) queryParamAnnotation).value(), parameter.getType());
                    }

                    Annotation pathParamAnnotation = parameter.getAnnotation(javax.ws.rs.PathParam.class);
                    if (pathParamAnnotation != null) {
                        handlerHolder.params[i] = new PathParam(((javax.ws.rs.PathParam) pathParamAnnotation).value(), parameter.getType());
                    }

                    Annotation formParamAnnotation = parameter.getAnnotation(javax.ws.rs.FormParam.class);
                    if (formParamAnnotation != null) {
                        handlerHolder.params[i] = new FormParam(((javax.ws.rs.FormParam) formParamAnnotation).value(), parameter.getType());
                    }

                    Annotation headerParamAnnotation = parameter.getAnnotation(HeaderParam.class);
                    if (headerParamAnnotation != null) {
                        handlerHolder.params[i] = new RequestHeaderParam(((HeaderParam) headerParamAnnotation).value(), parameter.getType());
                    }

                    Annotation contextAnnotation = parameter.getAnnotation(Context.class);
                    if (contextAnnotation != null) {
                        handlerHolder.params[i] = new ContextParam(parameter.getType());
                    }

                    if (pathParamAnnotation == null && queryParamAnnotation == null && formParamAnnotation == null &&
                            headerParamAnnotation == null && contextAnnotation == null) {
                        handlerHolder.params[i] = new BodyParam(parameter.getName(), parameter.getType(), contentType);
                    }
                }

                processors.remove(handlerHolder);
                processors.add(handlerHolder);
            }
        }
    }

    public static void populateBody(HttpRequest req, URIDecoder uriDecoder) {
        if (req.getMethod() == HttpMethod.PUT || req.getMethod() == HttpMethod.POST) {
            if (req instanceof HttpContent) {
                uriDecoder.bodyData = ((HttpContent) req).content();
                uriDecoder.contentType = req.headers().get(HttpHeaders.Names.CONTENT_TYPE);
            }
        }
    }

    public static HandlerHolder findHandler(HttpMethod httpMethod, String path) {
        for (HandlerHolder handlerHolder : processors) {
            if (handlerHolder.httpMethod == httpMethod && handlerHolder.uriTemplate.matches(path)) {
                return handlerHolder;
            }
        }

        return null;
    }

    public static FullHttpResponse invoke(HandlerHolder handlerHolder, Object[] params) {
        try {
            return (FullHttpResponse) handlerHolder.method.invoke(handlerHolder.handler, params);
        } catch (Exception e) {
            log.error("Error invoking handler. Reason : {}.", e.getMessage());
            return Response.serverError(e.getMessage());
        }
    }

    public static String path(String uri) {
        int pathEndPos = uri.indexOf('?');
        if (pathEndPos < 0) {
            return uri;
        } else {
            return uri.substring(0, pathEndPos);
        }
    }

}
