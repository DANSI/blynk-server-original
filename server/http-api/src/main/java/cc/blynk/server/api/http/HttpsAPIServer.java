package cc.blynk.server.api.http;

import cc.blynk.server.Holder;
import cc.blynk.server.api.http.handlers.HttpAndBlynkProtocolUnificationHandler;
import cc.blynk.server.api.http.handlers.HttpAndWebSocketUnificatorHandler;
import cc.blynk.server.api.http.handlers.LetsEncryptHandler;
import cc.blynk.server.application.handlers.main.AppChannelStateHandler;
import cc.blynk.server.application.handlers.main.auth.AppLoginHandler;
import cc.blynk.server.application.handlers.main.auth.GetServerHandler;
import cc.blynk.server.application.handlers.main.auth.RegisterHandler;
import cc.blynk.server.application.handlers.sharing.auth.AppShareLoginHandler;
import cc.blynk.server.core.BaseServer;
import cc.blynk.server.handlers.common.UserNotLoggedHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 1/12/2015.
 */
public class HttpsAPIServer extends BaseServer {

    private final ChannelInitializer<SocketChannel> channelInitializer;

    public HttpsAPIServer(Holder holder) {
        super(holder.props.getProperty("listen.address"),
                holder.props.getIntProperty("https.port"), holder.transportTypeHolder);

        final AppChannelStateHandler appChannelStateHandler = new AppChannelStateHandler(holder.sessionDao);
        final RegisterHandler registerHandler = new RegisterHandler(holder);
        final AppLoginHandler appLoginHandler = new AppLoginHandler(holder);
        final AppShareLoginHandler appShareLoginHandler = new AppShareLoginHandler(holder);
        final UserNotLoggedHandler userNotLoggedHandler = new UserNotLoggedHandler();
        final GetServerHandler getServerHandler = new GetServerHandler(holder);

        final HttpAndWebSocketUnificatorHandler httpAndWebSocketUnificatorHandler =
                new HttpAndWebSocketUnificatorHandler(holder, port);
        final LetsEncryptHandler letsEncryptHandler = new LetsEncryptHandler(holder.sslContextHolder.contentHolder);

        channelInitializer = new ChannelInitializer<SocketChannel>() {
            @Override
            protected void initChannel(SocketChannel ch) throws Exception {
                ch.pipeline()
                .addLast("HttpsSslContext", holder.sslContextHolder.sslCtx.newHandler(ch.alloc()))
                .addLast("HttpAndBlynkProtocolUnificator", new HttpAndBlynkProtocolUnificationHandler(
                        holder,
                        appChannelStateHandler,
                        registerHandler,
                        appLoginHandler,
                        appShareLoginHandler,
                        userNotLoggedHandler,
                        getServerHandler,
                        httpAndWebSocketUnificatorHandler,
                        letsEncryptHandler));
            }
        };
    }

    @Override
    public ChannelInitializer<SocketChannel> getChannelInitializer() {
        return channelInitializer;
    }

    @Override
    protected String getServerName() {
        return "HTTPS API, WebSockets and Admin page";
    }

    @Override
    public void close() {
        System.out.println("Shutting down HTTPS API, WebSockets and Admin server...");
        super.close();
    }

}
