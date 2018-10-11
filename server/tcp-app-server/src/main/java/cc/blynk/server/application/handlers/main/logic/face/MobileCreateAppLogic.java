package cc.blynk.server.application.handlers.main.logic.face;

import cc.blynk.server.application.handlers.main.auth.MobileStateHolder;
import cc.blynk.server.core.model.auth.App;
import cc.blynk.server.core.model.serialization.JsonParser;
import cc.blynk.server.core.protocol.exceptions.IllegalCommandException;
import cc.blynk.server.core.protocol.exceptions.NotAllowedException;
import cc.blynk.server.core.protocol.model.messages.StringMessage;
import cc.blynk.utils.AppNameUtil;
import cc.blynk.utils.ArrayUtil;
import io.netty.channel.ChannelHandlerContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static cc.blynk.server.core.protocol.enums.Command.CREATE_APP;
import static cc.blynk.server.internal.CommonByteBufUtil.makeUTF8StringMessage;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 01.02.16.
 */
public final class MobileCreateAppLogic {

    private static final Logger log = LogManager.getLogger(MobileCreateAppLogic.class);

    private MobileCreateAppLogic() {
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

        newApp.id = AppNameUtil.generateAppId();

        if (newApp.isNotValid()) {
            throw new NotAllowedException("App is not valid.", message.id);
        }

        log.debug("Creating new app {}.", newApp);

        var user = state.user;

        if (user.profile.apps.length > 25) {
            throw new NotAllowedException("App limit is reached.", message.id);
        }

        for (App app : user.profile.apps) {
            if (app.id.equals(newApp.id)) {
                throw new NotAllowedException("App with same id already exists.", message.id);
            }
        }

        user.profile.apps = ArrayUtil.add(user.profile.apps, newApp, App.class);
        user.lastModifiedTs = System.currentTimeMillis();

        ctx.writeAndFlush(makeUTF8StringMessage(CREATE_APP, message.id, JsonParser.toJson(newApp)), ctx.voidPromise());
    }

}
