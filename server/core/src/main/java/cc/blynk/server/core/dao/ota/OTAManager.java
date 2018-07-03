package cc.blynk.server.core.dao.ota;

import cc.blynk.server.core.dao.UserKey;
import cc.blynk.server.core.model.DashBoard;
import cc.blynk.server.core.model.auth.User;
import cc.blynk.server.core.model.device.Device;
import cc.blynk.server.core.model.device.DeviceOtaInfo;
import cc.blynk.server.core.model.device.HardwareInfo;
import cc.blynk.server.core.protocol.model.messages.StringMessage;
import cc.blynk.utils.properties.ServerProperties;
import io.netty.channel.ChannelHandlerContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.ConcurrentHashMap;

import static cc.blynk.server.core.protocol.enums.Command.BLYNK_INTERNAL;
import static cc.blynk.server.internal.CommonByteBufUtil.makeASCIIStringMessage;
import static cc.blynk.utils.FileUtils.getPatternFromString;

/**
 * Very basic OTA manager implementation.
 * For now it could be used only by super admin and updates firmware for all devices.
 *
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 21.08.17.
 */
public class OTAManager {

    private static final Logger log = LogManager.getLogger(OTAManager.class);

    public final String serverHostUrl;
    private volatile OTAInfo allInfo;
    private final ConcurrentHashMap<UserKey, OTAInfo> otaInfos;
    private final String staticFilesFolder;

    public OTAManager(ServerProperties props) {
        String port = props.getProperty("http.port", "8080");
        this.serverHostUrl = "http://" + props.host + (port.equals("80") ? "" : (":" + port));
        this.staticFilesFolder = props.jarPath;
        this.otaInfos = new ConcurrentHashMap<>();
    }

    public void initiateHardwareUpdate(ChannelHandlerContext ctx, UserKey userKey,
                                       HardwareInfo newHardwareInfo, DashBoard dash, Device device) {
        OTAInfo otaInfo = getOtaInfoForHardware(userKey, newHardwareInfo, dash.name);
        if (otaInfo != null) {
            sendOtaCommand(ctx, device, otaInfo);
            log.info("Ota command is sent for user {} and device {}:{}.",
                    userKey.email, device.name, device.id);
        }
    }

    private OTAInfo getOtaInfoForHardware(UserKey userKey, HardwareInfo newHardwareInfo, String dashName) {
        if (isFirmwareVersionChanged(allInfo, newHardwareInfo)) {
            log.info("Device build : {}, firmware build : {}. Firmware update is required.",
                    newHardwareInfo.build, allInfo.build);
            return allInfo;
        }

        OTAInfo otaInfo = otaInfos.get(userKey);
        if (isValidOtaInfo(otaInfo, newHardwareInfo, dashName)) {
            return otaInfo;
        }

        return null;
    }

    private static boolean isValidOtaInfo(OTAInfo otaInfo, HardwareInfo hardwareInfo, String dashName) {
        return isFirmwareVersionChanged(otaInfo, hardwareInfo) && otaInfo.matches(dashName);
    }

    private static boolean isFirmwareVersionChanged(OTAInfo otaInfo, HardwareInfo newHardwareInfo) {
        return otaInfo != null && newHardwareInfo.build != null
                && !newHardwareInfo.build.equals(otaInfo.build);
    }

    private void sendOtaCommand(ChannelHandlerContext ctx, Device device, OTAInfo otaInfo) {
        StringMessage msg = makeASCIIStringMessage(BLYNK_INTERNAL, 7777, otaInfo.makeHardwareBody(serverHostUrl));
        if (ctx.channel().isWritable()) {
            device.deviceOtaInfo = new DeviceOtaInfo(otaInfo.initiatedBy,
                    otaInfo.initiatedAt, System.currentTimeMillis());
            ctx.write(msg, ctx.voidPromise());
        }
    }

    public void initiate(User initiator, UserKey userKey, String projectName, String pathToFirmware) {
        String build = fetchBuildNumber(pathToFirmware);
        this.otaInfos.put(userKey, new OTAInfo(initiator.email, pathToFirmware, build, projectName));
    }

    public void initiateForAll(User initiator, String pathToFirmware) {
        String build = fetchBuildNumber(pathToFirmware);
        this.allInfo = new OTAInfo(initiator.email, pathToFirmware, build, null);
        log.info("Ota initiated. {}", allInfo);
    }

    public static String getBuildPatternFromString(Path path) {
        try {
            return getPatternFromString(path, "\0" + "build" + "\0");
        } catch (IOException ioe) {
            log.error("Error getting pattern from file. Reason : {}", ioe.getMessage());
            throw new RuntimeException(ioe);
        }
    }

    private String fetchBuildNumber(String pathToFirmware) {
        Path path = Paths.get(staticFilesFolder, pathToFirmware);
        return getBuildPatternFromString(path);
    }

    public void stop(User user) {
        this.allInfo = null;
        otaInfos.clear();
        log.info("Ota stopped by {}.", user.email);
    }

}
