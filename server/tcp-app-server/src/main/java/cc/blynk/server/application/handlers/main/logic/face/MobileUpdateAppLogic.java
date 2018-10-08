package cc.blynk.server.application.handlers.main.logic.face;

import cc.blynk.server.application.handlers.main.auth.MobileStateHolder;
import cc.blynk.server.core.model.serialization.JsonParser;
import cc.blynk.server.core.protocol.exceptions.IllegalCommandException;
import cc.blynk.server.core.protocol.exceptions.NotAllowedException;
import cc.blynk.server.core.protocol.model.messages.StringMessage;
import io.netty.channel.ChannelHandlerContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static cc.blynk.server.internal.CommonByteBufUtil.ok;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 01.02.16.
 */
public final class MobileUpdateAppLogic {

    private static final Logger log = LogManager.getLogger(MobileUpdateAppLogic.class);

    private MobileUpdateAppLogic() {
    }

    public static void messageReceived(ChannelHandlerContext ctx, MobileStateHolder state,
                                       StringMessage message, int maxWidgetSize) {
        var appString = message.body;

        if (appString == null || appString.isEmpty()) {
            throw new IllegalCommandException("Income app message is empty.");
        }

        if (appString.length() > maxWidgetSize) {
            throw new NotAllowedException("App is larger then limit.", message.id);
        }

        var newApp = JsonParser.parseApp(appString, message.id);

        if (newApp.isNotValid()) {
            throw new NotAllowedException("App is not valid.", message.id);
        }

        log.debug("Creating new app {}.", newApp);

        var user = state.user;

        var existingApp = user.profile.getAppById(newApp.id);

        if (existingApp == null) {
            throw new NotAllowedException("App with passed is not exists.", message.id);
        }

        existingApp.update(newApp);

        user.lastModifiedTs = System.currentTimeMillis();

        ctx.writeAndFlush(ok(message.id), ctx.voidPromise());
    }

}
