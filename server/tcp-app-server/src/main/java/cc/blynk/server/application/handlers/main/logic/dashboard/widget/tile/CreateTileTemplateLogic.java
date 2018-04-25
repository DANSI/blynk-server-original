package cc.blynk.server.application.handlers.main.logic.dashboard.widget.tile;

import cc.blynk.server.application.handlers.main.auth.AppStateHolder;
import cc.blynk.server.core.model.serialization.JsonParser;
import cc.blynk.server.core.model.widgets.ui.tiles.DeviceTiles;
import cc.blynk.server.core.model.widgets.ui.tiles.TileTemplate;
import cc.blynk.server.core.protocol.exceptions.IllegalCommandException;
import cc.blynk.server.core.protocol.exceptions.NotAllowedException;
import cc.blynk.server.core.protocol.model.messages.StringMessage;
import cc.blynk.utils.ArrayUtil;
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
public final class CreateTileTemplateLogic {

    private static final Logger log = LogManager.getLogger(CreateTileTemplateLogic.class);

    private CreateTileTemplateLogic() {
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

        var newTileTemplate = JsonParser.parseTileTemplate(tileTemplateString, message.id);
        for (TileTemplate tileTemplate : deviceTiles.templates) {
            if (tileTemplate.id == newTileTemplate.id) {
                throw new NotAllowedException("tile template with same id already exists.", message.id);
            }
        }

        log.debug("Creating tile template {}.", tileTemplateString);

        deviceTiles.templates = ArrayUtil.add(deviceTiles.templates, newTileTemplate, TileTemplate.class);
        deviceTiles.recreateTilesIfNecessary(newTileTemplate, null);

        dash.cleanPinStorage(deviceTiles, true);

        ctx.writeAndFlush(ok(message.id), ctx.voidPromise());
    }

}
