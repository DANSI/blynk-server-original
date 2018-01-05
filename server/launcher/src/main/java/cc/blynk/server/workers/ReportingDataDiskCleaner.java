package cc.blynk.server.workers;

import cc.blynk.server.core.dao.ReportingDao;
import cc.blynk.server.core.dao.UserDao;
import cc.blynk.server.core.model.DashBoard;
import cc.blynk.server.core.model.DataStream;
import cc.blynk.server.core.model.auth.User;
import cc.blynk.server.core.model.widgets.Widget;
import cc.blynk.server.core.model.widgets.outputs.HistoryGraph;
import cc.blynk.server.core.model.widgets.outputs.graph.EnhancedHistoryGraph;
import cc.blynk.server.core.model.widgets.outputs.graph.GraphDataStream;
import cc.blynk.server.core.model.widgets.outputs.graph.GraphGranularityType;
import cc.blynk.server.core.model.widgets.ui.tiles.DeviceTiles;
import cc.blynk.server.core.model.widgets.ui.tiles.TileTemplate;
import cc.blynk.utils.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 04.01.18.
 */
public class ReportingDataDiskCleaner implements Runnable {

    private static final Logger log = LogManager.getLogger(ReportingDataDiskCleaner.class);

    private final UserDao userDao;
    private final ReportingDao reportingDao;

    private long lastStart;
    private final Set<String> doNotRemovePaths;

    public ReportingDataDiskCleaner(UserDao userDao, ReportingDao reportingDao) {
        this.userDao = userDao;
        this.reportingDao = reportingDao;
        this.lastStart = System.currentTimeMillis();
        this.doNotRemovePaths = new HashSet<>();

    }

    @Override
    public void run() {
        try {
            log.info("Starting removing unused reporting data...");

            long now = System.currentTimeMillis();
            //todo
            //actually, it is better to do not save data for such pins
            //but fow now this approach is simpler and quicker
            int result = process();
            long after = System.currentTimeMillis();

            lastStart = now;

            log.debug("Removed {} files. Time : {} ms.", result, after - now);
        } catch (Throwable t) {
            log.error("Error removing unused reporting data.", t);
        } finally {
            doNotRemovePaths.clear();
        }
    }

    private int process() {
        int removedFilesCounter = 0;
        for (User user : userDao.getUsers().values()) {
            if (user.isUpdated(lastStart)) {
                for (DashBoard dashBoard : user.profile.dashBoards) {
                    for (Widget widget : dashBoard.widgets) {
                        if (widget instanceof DeviceTiles) {
                            DeviceTiles deviceTiles = (DeviceTiles) widget;
                            for (TileTemplate tileTemplate : deviceTiles.templates) {
                                for (Widget tilesWidget : tileTemplate.widgets) {
                                    add(dashBoard.id, tilesWidget);
                                }
                            }
                        } else {
                            add(dashBoard.id, widget);
                        }
                    }
                }

                Path reportingFolderPath = reportingDao.getUserReportingFolderPath(user);
                try (DirectoryStream<Path> reportingFolder = Files.newDirectoryStream(reportingFolderPath, "*")) {
                    for (Path reportingFile : reportingFolder) {
                        if (!doNotRemovePaths.contains(reportingFile.getFileName().toString())) {
                            log.debug("Removing {}", reportingFile);
                            FileUtils.deleteQuietly(reportingFile);
                            removedFilesCounter++;
                        }
                    }
                } catch (Exception e) {
                    log.debug(e);
                }

            }
        }
        return removedFilesCounter;
    }

    //todo handle case with device selector
    private void add(int dashId, Widget widget) {
        if (widget instanceof HistoryGraph) {
            HistoryGraph historyGraph = (HistoryGraph) widget;
            add(dashId, historyGraph);
        } else if (widget instanceof EnhancedHistoryGraph) {
            EnhancedHistoryGraph enhancedHistoryGraph = (EnhancedHistoryGraph) widget;
            add(dashId, enhancedHistoryGraph);
        }
    }

    private void add(int dashId, EnhancedHistoryGraph graph) {
        for (GraphDataStream graphDataStream : graph.dataStreams) {
            if (graphDataStream != null && graphDataStream.dataStream != null && graphDataStream.dataStream.isValid()) {
                DataStream dataStream = graphDataStream.dataStream;
                for (GraphGranularityType type : GraphGranularityType.values()) {
                    String filename = ReportingDao.generateFilename(dashId,
                            graphDataStream.targetId,
                            dataStream.pinType.pintTypeChar, dataStream.pin, type.label);
                    doNotRemovePaths.add(filename);
                }
            }
        }
    }

    private void add(int dashId, HistoryGraph graph) {
        for (DataStream dataStream : graph.dataStreams) {
            if (dataStream.isValid()) {
                for (GraphGranularityType type : GraphGranularityType.values()) {
                    String filename = ReportingDao.generateFilename(dashId, graph.deviceId,
                            dataStream.pinType.pintTypeChar, dataStream.pin, type.label);
                    doNotRemovePaths.add(filename);
                }
            }
        }

    }

}
