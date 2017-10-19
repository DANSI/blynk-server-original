package cc.blynk.server.application.handlers.main.logic;

import cc.blynk.server.application.handlers.main.auth.AppStateHolder;
import cc.blynk.server.core.model.auth.App;
import cc.blynk.server.core.model.auth.User;
import cc.blynk.server.core.model.serialization.JsonParser;
import cc.blynk.server.core.protocol.exceptions.IllegalCommandException;
import cc.blynk.server.core.protocol.exceptions.NotAllowedException;
import cc.blynk.server.core.protocol.model.messages.StringMessage;
import cc.blynk.utils.AppNameUtil;
import cc.blynk.utils.ArrayUtil;
import cc.blynk.utils.StringUtils;
import io.netty.channel.ChannelHandlerContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static cc.blynk.server.core.protocol.enums.Command.CREATE_APP;
import static cc.blynk.server.internal.BlynkByteBufUtil.makeUTF8StringMessage;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 01.02.16.
 */
public class CreateAppLogic {

    private static final Logger log = LogManager.getLogger(CreateAppLogic.class);

    private final int maxWidgetSize;

    public CreateAppLogic(int maxWidgetSize) {
        this.maxWidgetSize = maxWidgetSize;
    }

    public void messageReceived(ChannelHandlerContext ctx, AppStateHolder state, StringMessage message) {
        String appString = message.body;

        if (appString == null || appString.isEmpty()) {
            throw new IllegalCommandException("Income app message is empty.");
        }

        if (appString.length() > maxWidgetSize) {
            throw new NotAllowedException("App is larger then limit.");
        }

        App newApp = JsonParser.parseApp(appString);

        newApp.id = AppNameUtil.BLYNK.toLowerCase() + StringUtils.randomString(8);

        newApp.validate();

        log.debug("Creating new app {}.", newApp);

        User user = state.user;

        if (user.profile.apps.length > 25) {
            throw new NotAllowedException("App with same id already exists.");
        }

        for (App app : user.profile.apps) {
            if (app.id.equals(newApp.id)) {
                throw new NotAllowedException("App with same id already exists.");
            }
        }

        user.profile.apps = ArrayUtil.add(user.profile.apps, newApp, App.class);
        user.lastModifiedTs = System.currentTimeMillis();

        ctx.writeAndFlush(makeUTF8StringMessage(CREATE_APP, message.id, JsonParser.toJson(newApp)), ctx.voidPromise());
    }

}
