package cc.blynk.server.application.handlers.main.logic.dashboard.widget.tile;

import cc.blynk.server.application.handlers.main.auth.AppStateHolder;
import cc.blynk.server.core.model.serialization.JsonParser;
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
public final class UpdateTileTemplateLogic {

    private static final Logger log = LogManager.getLogger(UpdateTileTemplateLogic.class);

    private UpdateTileTemplateLogic() {
    }

    public static void messageReceived(ChannelHandlerContext ctx, AppStateHolder state, StringMessage message) {
        var split = split3(message.body);

        if (split.length < 3) {
            throw new IllegalCommandException("Wrong income message format.");
        }

        var dashId = Integer.parseInt(split[0]);
        var widgetId = Long.parseLong(split[1]);
        var tileTemplateString = split[2];

        if (tileTemplateString == null || tileTemplateString.isEmpty()) {
            throw new IllegalCommandException("Income tile template message is empty.");
        }

        var user = state.user;
        var dash = user.profile.getDashByIdOrThrow(dashId);
        var widget = dash.getWidgetByIdOrThrow(widgetId);

        if (!(widget instanceof DeviceTiles)) {
            throw new IllegalCommandException("Income widget id is not DeviceTiles.");
        }

        var deviceTiles = (DeviceTiles) widget;

        TileTemplate newTileTemplate = JsonParser.parseTileTemplate(tileTemplateString, message.id);
        int existingTileTemplateIndex = deviceTiles.getTileTemplateIndexByIdOrThrow(newTileTemplate.id);
        TileTemplate existingTileTemplate = deviceTiles.templates[existingTileTemplateIndex];

        deviceTiles.recreateTilesIfNecessary(newTileTemplate, existingTileTemplate);

        log.debug("Updating tile template {}.", tileTemplateString);
        deviceTiles.replaceTileTemplate(newTileTemplate, existingTileTemplateIndex);

        dash.cleanPinStorage(deviceTiles, true, false);

        ctx.writeAndFlush(ok(message.id), ctx.voidPromise());
    }

}
