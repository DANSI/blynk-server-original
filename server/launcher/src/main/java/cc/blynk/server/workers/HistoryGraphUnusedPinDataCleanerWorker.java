package cc.blynk.server.workers;

import cc.blynk.server.core.dao.ReportingDiskDao;
import cc.blynk.server.core.dao.UserDao;
import cc.blynk.server.core.model.DashBoard;
import cc.blynk.server.core.model.DataStream;
import cc.blynk.server.core.model.auth.User;
import cc.blynk.server.core.model.widgets.Target;
import cc.blynk.server.core.model.widgets.Widget;
import cc.blynk.server.core.model.widgets.outputs.graph.GraphDataStream;
import cc.blynk.server.core.model.widgets.outputs.graph.GraphGranularityType;
import cc.blynk.server.core.model.widgets.outputs.graph.Superchart;
import cc.blynk.server.core.model.widgets.ui.tiles.DeviceTiles;
import cc.blynk.server.core.model.widgets.ui.tiles.TileTemplate;
import cc.blynk.server.internal.EmptyArraysUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashSet;
import java.util.Set;

/**
 * Daily job used to clean reporting data that is not used by the history graphs
 * but stored anyway on the disk.
 *
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 04.01.18.
 */
public class HistoryGraphUnusedPinDataCleanerWorker implements Runnable {

    private static final Logger log = LogManager.getLogger(HistoryGraphUnusedPinDataCleanerWorker.class);

    private final UserDao userDao;
    private final ReportingDiskDao reportingDao;

    private long lastStart;

    public HistoryGraphUnusedPinDataCleanerWorker(UserDao userDao, ReportingDiskDao reportingDao) {
        this.userDao = userDao;
        this.reportingDao = reportingDao;
        this.lastStart = System.currentTimeMillis();

    }

    @Override
    public void run() {
        try {
            log.info("Start removing unused reporting data...");

            long now = System.currentTimeMillis();
            int result = removeUnsedInHistoryGraphData();

            lastStart = now;

            log.info("Removed {} files. Time : {} ms.", result, System.currentTimeMillis() - now);
        } catch (Throwable t) {
            log.error("Error removing unused reporting data.", t);
        }
    }

    private int removeUnsedInHistoryGraphData() {
        int removedFilesCounter = 0;
        Set<String> doNotRemovePaths = new HashSet<>();

        for (User user : userDao.getUsers().values()) {
            //we don't want to do a lot of work here,
            //so we check only active profiles that actually write data
            if (user.isUpdated(lastStart)) {
                doNotRemovePaths.clear();
                try {
                    for (DashBoard dashBoard : user.profile.dashBoards) {
                        for (Widget widget : dashBoard.widgets) {
                            if (widget instanceof DeviceTiles) {
                                DeviceTiles deviceTiles = (DeviceTiles) widget;
                                for (TileTemplate tileTemplate : deviceTiles.templates) {
                                    for (Widget tilesWidget : tileTemplate.widgets) {
                                        add(doNotRemovePaths, dashBoard, tilesWidget, tileTemplate.deviceIds);
                                    }
                                }
                            } else {
                                add(doNotRemovePaths, dashBoard, widget, null);
                            }
                        }
                    }

                    removedFilesCounter += reportingDao.delete(user,
                            reportingFile -> !doNotRemovePaths.contains(reportingFile.getFileName().toString()));
                } catch (Exception e) {
                    log.error("Error cleaning reporting record for user {}. {}", user.email, e.getMessage());
                }
            }
        }
        return removedFilesCounter;
    }

    private static void add(Set<String> doNotRemovePaths, DashBoard dash, Widget widget, int[] deviceIds) {
        if (widget instanceof Superchart) {
            Superchart enhancedHistoryGraph = (Superchart) widget;
            add(doNotRemovePaths, dash, enhancedHistoryGraph, deviceIds);
        }
    }

    private static void add(Set<String> doNotRemovePaths, DashBoard dash, Superchart graph, int[] deviceIds) {
        for (GraphDataStream graphDataStream : graph.dataStreams) {
            if (graphDataStream != null && graphDataStream.dataStream != null && graphDataStream.dataStream.isValid()) {
                DataStream dataStream = graphDataStream.dataStream;

                int[] resultIds;
                if (deviceIds == null) {
                    Target target = dash.getTarget(graphDataStream.targetId);
                    if (target != null) {
                        resultIds = target.getAssignedDeviceIds();
                    } else {
                        resultIds = EmptyArraysUtil.EMPTY_INTS;
                    }
                } else {
                    resultIds = deviceIds;
                }

                for (int deviceId : resultIds) {
                    for (GraphGranularityType type : GraphGranularityType.values()) {
                        String filename = ReportingDiskDao.generateFilename(dash.id,
                                deviceId,
                                dataStream.pinType, dataStream.pin, type);
                        doNotRemovePaths.add(filename);
                    }
                }
            }
        }
    }

}
