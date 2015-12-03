package cc.blynk.server.handlers.hardware.http;

import cc.blynk.server.model.DashBoard;
import cc.blynk.server.model.widgets.Widget;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpRequest;

import static io.netty.handler.codec.http.HttpResponseStatus.*;
import static io.netty.handler.codec.http.HttpVersion.*;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 03.12.15.
 */
public class HttpRequestHandler {

    private static final String JSON = "application/json";
    private static final String PLAIN_TEXT = "text/plain";

    public static FullHttpResponse processRequest(ChannelHandlerContext ctx, DashBoard dash, HttpRequest req, String[] paths) {
        switch (req.getMethod().name()) {
            case "GET" :
                return GET(paths, dash);
            case "POST" :
                return POST(paths, dash);
            default :
                return new DefaultFullHttpResponse(HTTP_1_1, NOT_FOUND);
        }
    }

    private static FullHttpResponse GET(String[] paths, DashBoard dash) {
        switch (paths[2].toLowerCase()) {
            /*
                GET /token/dashboard - get dashboard relatad with token
             */
            case "dashboard" :
                return new HttpResponse(HTTP_1_1, OK, dash.toString(), JSON);

            /*
                GET /token/v1 - fetch data from pin
             */
            default:
                GetUri getData = new GetUri(paths);
                Widget widget = dash.findWidgetByPin(getData.pin.pin, getData.pin.pinType);
                String value = widget.getValue(getData.pin.pin, getData.pin.pinType);
                return new HttpResponse(HTTP_1_1, OK, value, PLAIN_TEXT);
        }
    }

    private static FullHttpResponse POST(String[] paths, DashBoard dashBoard) {
        return new HttpResponse(HTTP_1_1, OK, null, PLAIN_TEXT);
    }

}
