package cc.blynk.server.core.dao.ota;

import cc.blynk.server.core.model.auth.User;
import cc.blynk.server.core.model.device.Device;
import cc.blynk.server.core.model.device.DeviceOtaInfo;
import cc.blynk.server.core.model.device.HardwareInfo;
import cc.blynk.utils.FileUtils;
import cc.blynk.utils.ServerProperties;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.file.Path;
import java.nio.file.Paths;

import static cc.blynk.server.core.protocol.enums.Command.BLYNK_INTERNAL;
import static cc.blynk.utils.BlynkByteBufUtil.makeASCIIStringMessage;
import static cc.blynk.utils.StringUtils.BODY_SEPARATOR;

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

    private volatile OTAInfo info;
    private final String serverHostUrl;
    private final String staticFilesFolder;

    public OTAManager(ServerProperties props) {
        this.serverHostUrl = "http://" + props.getServerHost();
        this.staticFilesFolder = ServerProperties.jarPath;
    }

    public boolean isUpdateRequired(HardwareInfo newHardwareInfo) {
        if (info != null && newHardwareInfo.build != null && !newHardwareInfo.build.equals(info.build)) {
            log.info("Device build : {}, firmware build : {}. Firmware update is required.", newHardwareInfo.build, info.build);
            return true;
        }
        return false;
    }

    public void sendOtaCommand(ChannelHandlerContext ctx, Device device) {
        ByteBuf msg = makeASCIIStringMessage(BLYNK_INTERNAL, 7777, info.firmwareInitCommandBody);
        if (ctx.channel().isWritable()) {
            device.deviceOtaInfo = new DeviceOtaInfo(info.initiatedBy, info.initiatedAt, System.currentTimeMillis());
            ctx.write(msg, ctx.voidPromise());
        }
    }

    public void initiate(User user, String pathToFirmware) {
        String otaInitCommandBody = buildOTAInitCommandBody(pathToFirmware);

        //todo this is ugly. but for now is ok.
        Path path = Paths.get(staticFilesFolder, pathToFirmware);
        String build = FileUtils.getBuildPatternFromString(path);

        this.info = new OTAInfo(System.currentTimeMillis(), user.email, otaInitCommandBody, build);
        log.info("Ota initiated. {}", info);
    }

    public String buildOTAInitCommandBody(String pathToFirmware) {
        return "ota" + BODY_SEPARATOR + serverHostUrl + pathToFirmware;
    }

}
