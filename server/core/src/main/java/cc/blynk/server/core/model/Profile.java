package cc.blynk.server.core.model;

import cc.blynk.server.core.model.auth.App;
import cc.blynk.server.core.model.device.Tag;
import cc.blynk.server.core.model.enums.PinType;
import cc.blynk.server.core.model.serialization.JsonParser;
import cc.blynk.server.core.model.widgets.Target;
import cc.blynk.server.core.model.widgets.Widget;
import cc.blynk.server.core.model.widgets.outputs.graph.GraphDataStream;
import cc.blynk.server.core.model.widgets.outputs.graph.Superchart;
import cc.blynk.server.core.model.widgets.ui.DeviceSelector;
import cc.blynk.server.core.model.widgets.ui.reporting.ReportingWidget;
import cc.blynk.server.core.model.widgets.ui.tiles.DeviceTiles;
import cc.blynk.server.core.model.widgets.ui.tiles.TileTemplate;
import cc.blynk.server.core.protocol.exceptions.IllegalCommandException;
import cc.blynk.utils.ArrayUtil;

import java.util.Arrays;

import static cc.blynk.server.internal.EmptyArraysUtil.EMPTY_APPS;
import static cc.blynk.server.internal.EmptyArraysUtil.EMPTY_DASHBOARDS;

/**
 * User: ddumanskiy
 * Date: 21.11.13
 * Time: 13:04
 */
public class Profile {

    public volatile DashBoard[] dashBoards = EMPTY_DASHBOARDS;

    public volatile App[] apps = EMPTY_APPS;

    public static Widget getWidgetWithLoggedPin(DashBoard dash, int deviceId, short pin, PinType pinType) {
        for (Widget widget : dash.widgets) {
            if (widget instanceof Superchart) {
                Superchart graph = (Superchart) widget;
                if (isWithinGraph(dash, graph, pin, pinType, deviceId)) {
                    return graph;
                }
            }
            if (widget instanceof DeviceTiles) {
                DeviceTiles deviceTiles = (DeviceTiles) widget;
                for (TileTemplate tileTemplate : deviceTiles.templates) {
                    for (Widget tilesWidget : tileTemplate.widgets) {
                        if (tilesWidget instanceof Superchart) {
                            Superchart graph = (Superchart) tilesWidget;
                            if (isWithinGraph(dash, graph, pin, pinType, deviceId, tileTemplate.deviceIds)) {
                                return graph;
                            }
                        }
                    }
                }
            }
            if (widget instanceof ReportingWidget) {
                ReportingWidget reportingWidget = (ReportingWidget) widget;
                if (reportingWidget.hasPin(pin, pinType)) {
                    return reportingWidget;
                }
            }
        }
        return null;
    }

    private static boolean isWithinGraph(DashBoard dash, Superchart graph,
                                         short pin, PinType pinType, int deviceId, int... deviceIds) {
        for (GraphDataStream graphDataStream : graph.dataStreams) {
            if (graphDataStream != null && graphDataStream.dataStream != null
                    && graphDataStream.dataStream.isSame(pin, pinType)) {

                int graphTargetId = graphDataStream.targetId;

                //this is the case when datastream assigned directly to the device
                if (deviceId == graphTargetId) {
                    return true;
                }

                //this is the case when graph is within deviceTiles
                if (deviceIds != null && ArrayUtil.contains(deviceIds, deviceId)) {
                    return true;
                }

                //this is the case when graph is within device selector or tags
                Target target;
                if (graphTargetId < Tag.START_TAG_ID) {
                    target = dash.getDeviceById(graphTargetId);
                } else if (graphTargetId < DeviceSelector.DEVICE_SELECTOR_STARTING_ID) {
                    target = dash.getTagById(graphTargetId);
                } else {
                    //means widget assigned to device selector widget.
                    target = dash.getDeviceSelector(graphTargetId);
                }
                if (target != null && ArrayUtil.contains(target.getAssignedDeviceIds(), deviceId)) {
                    return true;
                }
            }
        }
        return false;
    }

    public int getDashIndexOrThrow(int dashId) {
        for (int i = 0; i < dashBoards.length; i++) {
            if (dashBoards[i].id == dashId) {
                return i;
            }
        }
        throw new IllegalCommandException("Dashboard with passed id not found.");
    }

    public DashBoard getDashByIdOrThrow(int id) {
        for (DashBoard dashBoard : dashBoards) {
            if (dashBoard.id == id) {
                return dashBoard;
            }
        }
        throw new IllegalCommandException("Dashboard with passed id not found.");
    }

    public DashBoard getDashById(int id) {
        for (DashBoard dashBoard : dashBoards) {
            if (dashBoard.id == id) {
                return dashBoard;
            }
        }
        return null;
    }

    public int getAppIndexById(String id) {
        for (int i = 0; i < apps.length; i++) {
            if (apps[i].id.equals(id)) {
                return i;
            }
        }
        return -1;
    }

    public App getAppById(String id) {
        for (App app : apps) {
            if (app.id.equals(id)) {
                return app;
            }
        }
        return null;
    }

    @Override
    public String toString() {
        return JsonParser.toJson(this);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Profile that = (Profile) o;

        return Arrays.equals(dashBoards, that.dashBoards);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(dashBoards);
    }
}
