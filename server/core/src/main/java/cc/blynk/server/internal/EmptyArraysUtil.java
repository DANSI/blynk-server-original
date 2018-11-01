package cc.blynk.server.internal;

import cc.blynk.server.core.model.DashBoard;
import cc.blynk.server.core.model.auth.App;
import cc.blynk.server.core.model.device.Device;
import cc.blynk.server.core.model.device.Tag;
import cc.blynk.server.core.model.widgets.Widget;
import cc.blynk.server.core.model.widgets.outputs.graph.GraphDataStream;
import cc.blynk.server.core.model.widgets.ui.reporting.Report;
import cc.blynk.server.core.model.widgets.ui.reporting.source.ReportDataStream;
import cc.blynk.server.core.model.widgets.ui.reporting.source.ReportSource;
import cc.blynk.server.core.model.widgets.ui.tiles.Tile;
import cc.blynk.server.core.model.widgets.ui.tiles.TileTemplate;
import io.netty.util.internal.EmptyArrays;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 30.09.17.
 */
public final class EmptyArraysUtil {

    private EmptyArraysUtil() {
    }

    public static final int[] EMPTY_INTS = {};
    public static final DashBoard[] EMPTY_DASHBOARDS = {};
    public static final Tag[] EMPTY_TAGS = {};
    public static final Device[] EMPTY_DEVICES = {};
    public static final Widget[] EMPTY_WIDGETS = {};
    public static final TileTemplate[] EMPTY_TEMPLATES = {};
    public static final Tile[] EMPTY_DEVICE_TILES = {};
    public static final byte[] EMPTY_BYTES = EmptyArrays.EMPTY_BYTES;
    public static final App[] EMPTY_APPS = {};
    public static final GraphDataStream[] EMPTY_GRAPH_DATA_STREAMS = {};
    public static final ReportDataStream[] EMPTY_REPORT_DATA_STREAMS = {};
    public static final ReportSource[] EMPTY_REPORT_SOURCES = {};
    public static final Report[] EMPTY_REPORTS = {};

}
