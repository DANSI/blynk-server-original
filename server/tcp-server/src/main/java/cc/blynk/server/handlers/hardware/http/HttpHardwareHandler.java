package cc.blynk.server.handlers.hardware.http;

import cc.blynk.server.dao.UserDao;
import cc.blynk.server.model.DashBoard;
import cc.blynk.server.model.auth.User;
import cc.blynk.server.model.widgets.Widget;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpRequest;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static io.netty.handler.codec.http.HttpHeaders.Names.*;
import static io.netty.handler.codec.http.HttpResponseStatus.*;
import static io.netty.handler.codec.http.HttpVersion.*;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 01.12.15.
 */
public class HttpHardwareHandler extends ChannelInboundHandlerAdapter {

    private static final Logger log = LogManager.getLogger(HttpHardwareHandler.class);

    private static final String JSON = "application/json";
    private static final String PLAIN_TEXT = "plain/text";

    private final UserDao userDao;

    public HttpHardwareHandler(UserDao userDao) {
        this.userDao = userDao;
    }

    private static void send(ChannelHandlerContext ctx, HttpRequest req, FullHttpResponse response) {
        if (!HttpHeaders.isKeepAlive(req)) {
            ctx.write(response).addListener(ChannelFutureListener.CLOSE);
        } else {
            response.headers().set(CONNECTION, HttpHeaders.Values.KEEP_ALIVE);
            ctx.write(response);
        }
    }

    private static void processRequest(ChannelHandlerContext ctx, DashBoard dash, HttpRequest req, String[] paths) {
        switch (req.getMethod().name()) {
            case "POST" :
                send(ctx, req, POST(paths, dash));
                break;
            case "GET" :
                send(ctx, req, GET(paths, dash));
                break;
        }
    }

    private static FullHttpResponse POST(String[] paths, DashBoard dashBoard) {
        return new HttpResponse(HTTP_1_1, OK, null, PLAIN_TEXT);
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

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        if (!(msg instanceof HttpRequest)) {
            return;
        }

        HttpRequest req = (HttpRequest) msg;

        String[] paths = req.getUri().split("/");
        String token = paths[1];

        User user = userDao.tokenManager.getUserByToken(token);

        if (user == null) {
            log.debug("HardwareLogic token is invalid. Token '{}', '{}'", token, ctx.channel().remoteAddress());
            send(ctx, req, new DefaultFullHttpResponse(HTTP_1_1, UNAUTHORIZED));
            return;
        }

        Integer dashId = UserDao.getDashIdByToken(user.dashTokens, token, 0);
        DashBoard dash = user.profile.getDashById(dashId, 0);

        processRequest(ctx, dash, req, paths);

    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) {
        ctx.flush();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        log.error("aaa", cause);
        ctx.close();
    }
}
