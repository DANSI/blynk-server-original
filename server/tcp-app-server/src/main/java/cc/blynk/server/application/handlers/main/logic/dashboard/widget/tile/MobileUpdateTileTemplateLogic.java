package cc.blynk.server.application.handlers.main.logic.dashboard.widget.tile;

import cc.blynk.server.application.handlers.main.auth.MobileStateHolder;
import cc.blynk.server.core.model.DashBoard;
import cc.blynk.server.core.model.auth.User;
import cc.blynk.server.core.model.serialization.JsonParser;
import cc.blynk.server.core.model.widgets.Widget;
import cc.blynk.server.core.model.widgets.ui.tiles.DeviceTiles;
import cc.blynk.server.core.model.widgets.ui.tiles.TileTemplate;
import cc.blynk.server.core.protocol.exceptions.IllegalCommandException;
import cc.blynk.server.core.protocol.model.messages.StringMessage;
import io.netty.channel.ChannelHandlerContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static cc.blynk.server.internal.CommonByteBufUtil.ok;
import static cc.blynk.utils.StringUtils.split3;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 01.02.16.
 */
public final class MobileUpdateTileTemplateLogic {

    private static final Logger log = LogManager.getLogger(MobileUpdateTileTemplateLogic.class);

    private MobileUpdateTileTemplateLogic() {
    }

    public static void messageReceived(ChannelHandlerContext ctx, MobileStateHolder state, StringMessage message) {
        String[] split = split3(message.body);

        if (split.length < 3) {
            throw new IllegalCommandException("Wrong income message format.");
        }

        int dashId = Integer.parseInt(split[0]);
        long widgetId = Long.parseLong(split[1]);
        String tileTemplateString = split[2];

        if (tileTemplateString == null || tileTemplateString.isEmpty()) {
            throw new IllegalCommandException("Income tile template message is empty.");
        }

        User user = state.user;
        DashBoard dash = user.profile.getDashByIdOrThrow(dashId);
        Widget widget = dash.getWidgetByIdOrThrow(widgetId);

        if (!(widget instanceof DeviceTiles)) {
            throw new IllegalCommandException("Income widget id is not DeviceTiles.");
        }

        DeviceTiles deviceTiles = (DeviceTiles) widget;

        TileTemplate newTileTemplate = JsonParser.parseTileTemplate(tileTemplateString, message.id);
        int existingTileTemplateIndex = deviceTiles.getTileTemplateIndexByIdOrThrow(newTileTemplate.id);
        TileTemplate existingTileTemplate = deviceTiles.templates[existingTileTemplateIndex];

        deviceTiles.recreateTilesIfNecessary(newTileTemplate, existingTileTemplate);

        log.debug("Updating tile template {}.", tileTemplateString);
        deviceTiles.replaceTileTemplate(newTileTemplate, existingTileTemplateIndex);

        user.profile.cleanPinStorage(dash, deviceTiles, false);

        ctx.writeAndFlush(ok(message.id), ctx.voidPromise());
    }

}
