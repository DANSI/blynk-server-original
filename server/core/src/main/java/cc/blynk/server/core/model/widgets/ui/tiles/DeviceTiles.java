package cc.blynk.server.core.model.widgets.ui.tiles;

import cc.blynk.server.core.model.DataStream;
import cc.blynk.server.core.model.enums.PinMode;
import cc.blynk.server.core.model.enums.PinType;
import cc.blynk.server.core.model.widgets.AppSyncWidget;
import cc.blynk.server.core.model.widgets.Widget;
import cc.blynk.server.core.protocol.exceptions.IllegalCommandException;
import cc.blynk.utils.ArrayUtil;
import io.netty.channel.Channel;

import java.util.ArrayList;
import java.util.Arrays;

import static cc.blynk.server.core.protocol.enums.Command.APP_SYNC;
import static cc.blynk.server.internal.BlynkByteBufUtil.makeUTF8StringMessage;
import static cc.blynk.server.internal.EmptyArraysUtil.EMPTY_DEVICE_TILES;
import static cc.blynk.server.internal.EmptyArraysUtil.EMPTY_TEMPLATES;
import static cc.blynk.utils.StringUtils.prependDashIdAndDeviceId;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 02.10.17.
 */
public class DeviceTiles extends Widget implements AppSyncWidget {

    public volatile TileTemplate[] templates = EMPTY_TEMPLATES;

    public volatile DeviceTile[] tiles = EMPTY_DEVICE_TILES;

    //this field is needed only in the realtime when users selects some template for the device
    //so we know what reading widgets should update their state at that moment
    public transient int selectedDeviceId;

    public int rows;

    public int columns;

    public SortType sortType;

    public void deleteDeviceTilesByTemplateId(long deviceTileId) {
        ArrayList<DeviceTile> list = new ArrayList<>();
        for (DeviceTile tile : tiles) {
            if (tile.templateId != deviceTileId) {
                list.add(tile);
            }
        }
        tiles = list.toArray(new DeviceTile[list.size()]);
    }

    public TileTemplate findTemplateByDeviceId(int deviceId) {
        for (TileTemplate tileTemplate : templates) {
            if (ArrayUtil.contains(tileTemplate.deviceIds, deviceId)) {
                return tileTemplate;
            }
        }
        return null;
    }

    public void recreateTilesIfNecessary(TileTemplate newTileTemplate, TileTemplate existingTileTemplate) {
        //no changes. do nothing.
        if (existingTileTemplate != null
                && Arrays.equals(newTileTemplate.deviceIds, existingTileTemplate.deviceIds)
                && newTileTemplate.dataStream != null
                && newTileTemplate.dataStream.equals(existingTileTemplate.dataStream)) {
            return;
        }

        ArrayList<DeviceTile> list = new ArrayList<>();
        for (TileTemplate tileTemplate : this.templates) {
            //creating new device tiles for updated TileTemplate
            if (tileTemplate.id == newTileTemplate.id) {
                for (int deviceId : newTileTemplate.deviceIds) {
                    list.add(
                            new DeviceTile(
                                    deviceId,
                                    tileTemplate.id,
                                    newTileTemplate.dataStream == null
                                            ? null
                                            : new DataStream(newTileTemplate.dataStream)
                            )
                    );
                }
            //leaving untouched device tiles that are not updated
            } else {
                for (DeviceTile deviceTile : this.tiles) {
                    if (deviceTile.templateId == tileTemplate.id) {
                        list.add(deviceTile);
                    }
                }
            }
        }
        this.tiles = list.toArray(new DeviceTile[list.size()]);
    }

    public int getTileTemplateIndexByIdOrThrow(long id) {
        for (int i = 0; i < templates.length; i++) {
            if (templates[i].id == id) {
                return i;
            }
        }
        throw new IllegalCommandException("Tile template with passed id not found.");
    }

    public Widget getWidgetById(long widgetId) {
        for (TileTemplate tileTemplate : templates) {
            if (tileTemplate.widgets != null) {
                for (Widget widget : tileTemplate.widgets) {
                    if (widget.id == widgetId) {
                        return widget;
                    }
                }
            }
        }
        return null;
    }

    @Override
    public boolean updateIfSame(int deviceId, byte pin, PinType pinType, String value) {
        for (DeviceTile deviceTile : tiles) {
            if (deviceTile.updateIfSame(deviceId, pin, pinType, value)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void sendAppSync(Channel appChannel, int dashId, int targetId) {
        for (DeviceTile tile : tiles) {
            if (tile.deviceId == targetId && tile.dataStream != null && tile.dataStream.notEmpty()) {
                String hardBody = tile.dataStream.makeHardwareBody();
                String body = prependDashIdAndDeviceId(dashId, targetId, hardBody);
                appChannel.write(makeUTF8StringMessage(APP_SYNC, SYNC_DEFAULT_MESSAGE_ID, body));
            }
        }
    }

    @Override
    public PinMode getModeType() {
        return null;
    }

    @Override
    public int getPrice() {
        return 4900;
    }
}
