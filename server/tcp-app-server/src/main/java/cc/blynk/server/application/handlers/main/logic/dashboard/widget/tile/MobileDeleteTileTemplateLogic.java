package cc.blynk.server.application.handlers.main.logic.dashboard.widget.tile;

import cc.blynk.server.application.handlers.main.auth.MobileStateHolder;
import cc.blynk.server.core.model.DashBoard;
import cc.blynk.server.core.model.auth.User;
import cc.blynk.server.core.model.widgets.Widget;
import cc.blynk.server.core.model.widgets.ui.tiles.DeviceTiles;
import cc.blynk.server.core.model.widgets.ui.tiles.TileTemplate;
import cc.blynk.server.core.protocol.exceptions.IllegalCommandException;
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
public final class MobileDeleteTileTemplateLogic {

    private static final Logger log = LogManager.getLogger(MobileDeleteTileTemplateLogic.class);

    private MobileDeleteTileTemplateLogic() {
    }

    public static void messageReceived(ChannelHandlerContext ctx, MobileStateHolder state, StringMessage message) {
        String[] split = split3(message.body);

        if (split.length < 2) {
            throw new IllegalCommandException("Wrong income message format.");
        }

        int dashId = Integer.parseInt(split[0]);
        long widgetId = Long.parseLong(split[1]);
        long tileId = Long.parseLong(split[2]);

        User user = state.user;
        DashBoard dash = user.profile.getDashByIdOrThrow(dashId);
        Widget widget = dash.getWidgetByIdOrThrow(widgetId);

        if (!(widget instanceof DeviceTiles)) {
            throw new IllegalCommandException("Income widget id is not DeviceTiles.");
        }

        DeviceTiles deviceTiles = (DeviceTiles) widget;
        int existingTileIndex = deviceTiles.getTileTemplateIndexByIdOrThrow(tileId);

        log.debug("Deleting tile template dashId : {}, widgetId : {}, tileId : {}.", dash, widgetId, tileId);

        TileTemplate tileTemplate = deviceTiles.templates[existingTileIndex];
        user.addEnergy(tileTemplate.getPrice());

        deviceTiles.templates = ArrayUtil.remove(deviceTiles.templates, existingTileIndex, TileTemplate.class);
        deviceTiles.deleteDeviceTilesByTemplateId(tileId);
        user.profile.cleanPinStorageForTileTemplate(dash, tileTemplate, true);

        dash.updatedAt = System.currentTimeMillis();

        ctx.writeAndFlush(ok(message.id), ctx.voidPromise());
    }

}
