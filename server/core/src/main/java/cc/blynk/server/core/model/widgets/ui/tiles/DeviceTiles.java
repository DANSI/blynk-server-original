package cc.blynk.server.core.model.widgets.ui.tiles;

import cc.blynk.server.core.model.DataStream;
import cc.blynk.server.core.model.enums.PinType;
import cc.blynk.server.core.model.widgets.Widget;
import cc.blynk.server.core.protocol.exceptions.IllegalCommandException;

import java.util.ArrayList;
import java.util.Arrays;

import static cc.blynk.server.internal.EmptyArraysUtil.EMPTY_DEVICE_TILES;
import static cc.blynk.server.internal.EmptyArraysUtil.EMPTY_TEMPLATES;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 02.10.17.
 */
public class DeviceTiles extends Widget {

    public volatile TileTemplate[] templates = EMPTY_TEMPLATES;

    public volatile DeviceTile[] tiles = EMPTY_DEVICE_TILES;

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

    public void recreateTilesIfNecessary(TileTemplate newTileTemplate, TileTemplate existingTileTemplate) {
        //no changes. do nothing.
        if (existingTileTemplate != null
                && Arrays.equals(newTileTemplate.deviceIds, existingTileTemplate.deviceIds)) {
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
                                    tileTemplate.dataStream == null
                                            ? null
                                            : new DataStream(tileTemplate.dataStream)
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
    public void updateIfSame(Widget widget) {
        //todo finish
    }

    @Override
    public boolean isSame(int deviceId, byte pin, PinType type) {
        //todo finish
        return false;
    }

    @Override
    public String getJsonValue() {
        //todo finish
        return null;
    }

    @Override
    public String getModeType() {
        return "in";
    }

    @Override
    public void append(StringBuilder sb, int deviceId) {

    }

    @Override
    public int getPrice() {
        return 4900;
    }
}
