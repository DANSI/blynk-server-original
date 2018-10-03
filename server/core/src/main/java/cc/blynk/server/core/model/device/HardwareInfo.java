package cc.blynk.server.core.model.device;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * This is piece of the information that hardware sends to the server
 * via "internal" command right after it is connected to the Blynk Cloud.
 *
 * May be absent in some cases (old firmware, java,  js, python clients)
 *
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 18.05.16.
 */
public class HardwareInfo {

    public static final int DEFAULT_HARDWARE_BUFFER_SIZE = 255 - 5; //5 is for blynk header

    public final String version;

    public final String blynkVersion;

    public final String boardType;

    public final String cpuType;

    public final String connectionType;

    public final String build;

    public final String templateId;

    public final int heartbeatInterval;

    public final int buffIn;

    @JsonCreator
    public HardwareInfo(@JsonProperty("version") String version,
                        @JsonProperty("blynkVersion") String blynkVersion,
                        @JsonProperty("boardType") String boardType,
                        @JsonProperty("cpuType") String cpuType,
                        @JsonProperty("connectionType") String connectionType,
                        @JsonProperty("build") String build,
                        @JsonProperty("templateId") String templateId,
                        @JsonProperty("heartbeatInterval") int heartbeatInterval,
                        @JsonProperty("buffIn") int buffIn) {
        this.version = version;
        this.blynkVersion = blynkVersion;
        this.boardType = boardType;
        this.cpuType = cpuType;
        this.connectionType = connectionType;
        this.build = build;
        this.templateId = templateId;
        this.heartbeatInterval = heartbeatInterval;
        this.buffIn = buffIn <= 0 ? DEFAULT_HARDWARE_BUFFER_SIZE : buffIn;
    }

    public HardwareInfo(String[] info) {
        HardwareInfoPrivate hardwareInfoPrivate = new HardwareInfoPrivate(info);
        this.version = hardwareInfoPrivate.version;
        this.blynkVersion = hardwareInfoPrivate.blynkVersion;
        this.boardType = hardwareInfoPrivate.boardType;
        this.cpuType = hardwareInfoPrivate.cpuType;
        this.connectionType = hardwareInfoPrivate.connectionType;
        this.build = hardwareInfoPrivate.build;
        this.templateId = hardwareInfoPrivate.templateId;
        this.heartbeatInterval = hardwareInfoPrivate.heartbeatInterval;
        this.buffIn = hardwareInfoPrivate.buffIn <= 0 ? DEFAULT_HARDWARE_BUFFER_SIZE : hardwareInfoPrivate.buffIn;
    }

}
