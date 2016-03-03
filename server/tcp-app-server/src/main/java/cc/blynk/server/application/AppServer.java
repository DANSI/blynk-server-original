package cc.blynk.server.application;

import cc.blynk.server.Holder;
import cc.blynk.server.application.handlers.main.AppChannelStateHandler;
import cc.blynk.server.application.handlers.main.auth.AppLoginHandler;
import cc.blynk.server.application.handlers.main.auth.RegisterHandler;
import cc.blynk.server.application.handlers.sharing.auth.AppShareLoginHandler;
import cc.blynk.server.core.BaseServer;
import cc.blynk.server.core.protocol.handlers.decoders.MessageDecoder;
import cc.blynk.server.core.protocol.handlers.encoders.MessageEncoder;
import cc.blynk.server.handlers.common.UserNotLoggedHandler;
import cc.blynk.utils.AppSslUtil;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.ssl.SslHandler;
import io.netty.handler.timeout.ReadTimeoutHandler;

import javax.net.ssl.SSLEngine;

/**
 * Class responsible for handling all Application connections and netty pipeline initialization.
 *
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 2/1/2015.
 */
public class AppServer extends BaseServer {

    private final ChannelInitializer<SocketChannel> channelInitializer;

    public AppServer(Holder holder) {
        super(holder.props.getIntProperty("app.ssl.port"));

        final RegisterHandler registerHandler = new RegisterHandler(holder.userDao, holder.props.getCommaSeparatedList("allowed.users.list"));
        final AppLoginHandler appLoginHandler = new AppLoginHandler(holder.props, holder.userDao, holder.sessionDao, holder.reportingDao, holder.blockingIOProcessor, holder.dbManager);
        final AppChannelStateHandler appChannelStateHandler = new AppChannelStateHandler(holder.sessionDao);
        final AppShareLoginHandler appShareLoginHandler = new AppShareLoginHandler(holder.props, holder.userDao, holder.sessionDao, holder.reportingDao, holder.blockingIOProcessor);
        final UserNotLoggedHandler userNotLoggedHandler = new UserNotLoggedHandler();

        log.info("Enabling SSL for application.");
        AppSslContext appSslContext = AppSslUtil.initMutualSslContext(holder.props);

        int appTimeoutSecs = holder.props.getIntProperty("app.socket.idle.timeout", 0);
        log.debug("app.socket.idle.timeout = {}", appTimeoutSecs);

        this.channelInitializer = new ChannelInitializer<SocketChannel>() {
            @Override
            protected void initChannel(SocketChannel ch) throws Exception {
                ChannelPipeline pipeline = ch.pipeline();

                if (appTimeoutSecs > 0) {
                    pipeline.addLast(new ReadTimeoutHandler(appTimeoutSecs));
                }

                final SSLEngine engine = appSslContext.sslContext.newEngine(ch.alloc());
                if (appSslContext.isMutualSSL) {
                    engine.setUseClientMode(false);
                    engine.setNeedClientAuth(true);
                }
                pipeline.addLast(
                    new SslHandler(engine),
                    appChannelStateHandler,
                    new MessageDecoder(holder.stats),
                    new MessageEncoder(holder.stats),
                    registerHandler,
                    appLoginHandler,
                    appShareLoginHandler,
                    userNotLoggedHandler
                );
            }
        };

        log.info("Application server port {}.", port);
    }

    @Override
    public ChannelInitializer<SocketChannel> getChannelInitializer() {
        return channelInitializer;
    }

    @Override
    protected String getServerName() {
        return "application";
    }

    @Override
    public void close() {
        System.out.println("Shutting down application SSL server...");
        super.close();
    }

}
