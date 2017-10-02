package cc.blynk.server.core.model.widgets.ui.tiles;

import cc.blynk.server.core.model.enums.PinType;
import cc.blynk.server.core.model.widgets.Widget;
import cc.blynk.server.core.protocol.exceptions.IllegalCommandException;

import static cc.blynk.server.internal.EmptyArraysUtil.EMPTY_TEMPLATES;
import static cc.blynk.server.internal.EmptyArraysUtil.EMPTY_TILES;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 02.10.17.
 */
public class DeviceTiles extends Widget {

    public volatile TileTemplate[] templates = EMPTY_TEMPLATES;

    public Tile[] tiles = EMPTY_TILES;

    public int rows;

    public int columns;

    public SortType sortType;

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
        for (Tile tile : tiles) {
            if (tile.updateIfSame(deviceId, pin, pinType, value)) {
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
