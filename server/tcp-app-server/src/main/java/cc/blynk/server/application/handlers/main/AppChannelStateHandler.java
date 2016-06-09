package cc.blynk.server.application.handlers.main;

import cc.blynk.server.application.handlers.main.auth.AppStateHolder;
import cc.blynk.server.core.dao.SessionDao;
import cc.blynk.server.core.model.auth.Session;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static cc.blynk.utils.AppStateHolderUtil.*;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 2/20/2015.
 *
 * Removes channel from session in case it became inactive (closed from client side).
 */
@ChannelHandler.Sharable
public class AppChannelStateHandler extends ChannelInboundHandlerAdapter {

    private static final Logger log = LogManager.getLogger(AppChannelStateHandler.class);

    private final SessionDao sessionDao;

    public AppChannelStateHandler(SessionDao sessionDao) {
        this.sessionDao = sessionDao;
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        AppStateHolder state = getAppState(ctx.channel());
        if (state != null) {
            Session session = sessionDao.userSession.get(state.user);
            if (session != null) {
                session.removeAppChannel(ctx.channel());
                log.trace("Application channel disconnect. {}", ctx.channel());
            }
        }
    }

}
