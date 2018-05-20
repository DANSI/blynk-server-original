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

    public final String boardType;

    public final String cpuType;

    public final String connectionType;

    public final String build;

    public final String templateId;

    public final int heartbeatInterval;

    public final int buffIn;

    @JsonCreator
    public HardwareInfo(@JsonProperty("version") String version,
                        @JsonProperty("boardType") String boardType,
                        @JsonProperty("cpuType") String cpuType,
                        @JsonProperty("connectionType") String connectionType,
                        @JsonProperty("build") String build,
                        @JsonProperty("templateId") String templateId,
                        @JsonProperty("heartbeatInterval") int heartbeatInterval,
                        @JsonProperty("buffIn") int buffIn) {
        this.version = version;
        this.boardType = boardType;
        this.cpuType = cpuType;
        this.connectionType = connectionType;
        this.build = build;
        this.templateId = templateId;
        this.heartbeatInterval = heartbeatInterval;
        this.buffIn = buffIn <= 0 ? DEFAULT_HARDWARE_BUFFER_SIZE : buffIn;;
    }

    public HardwareInfo(String[] info) {
        HardwareInfoPrivate hardwareInfoPrivate = new HardwareInfoPrivate(info);
        this.version = hardwareInfoPrivate.version;
        this.boardType = hardwareInfoPrivate.boardType;
        this.cpuType = hardwareInfoPrivate.cpuType;
        this.connectionType = hardwareInfoPrivate.connectionType;
        this.build = hardwareInfoPrivate.build;
        this.templateId = hardwareInfoPrivate.templateId;
        this.heartbeatInterval = hardwareInfoPrivate.heartbeatInterval;
        this.buffIn = hardwareInfoPrivate.buffIn <= 0 ? DEFAULT_HARDWARE_BUFFER_SIZE : hardwareInfoPrivate.buffIn;
    }

    //utility class to make fields of HardwareInfo final, used instead of hashmap
    private final class HardwareInfoPrivate {
        private String version;
        private String boardType;
        private String cpuType;
        private String connectionType;
        private String templateId;
        private String build;
        private int heartbeatInterval;
        private int buffIn;

        private HardwareInfoPrivate(String[] info) {
            for (int i = 0; i < info.length; i++) {
                if (i < info.length - 1) {
                    intiField(info[i], info[++i]);
                }
            }
        }

        private void intiField(final String key, final String value) {
            switch (key) {
                case "h-beat" :
                    try {
                        this.heartbeatInterval = Integer.parseInt(value);
                    } catch (NumberFormatException nfe) {
                        this.heartbeatInterval = -1;
                    }
                    break;
                case "ver" :
                    this.version = value;
                    break;
                case "dev" :
                    this.boardType = value;
                    break;
                case "cpu" :
                    this.cpuType = value;
                    break;
                case "con" :
                    this.connectionType = value;
                    break;
                case "tmpl" :
                    this.templateId = value;
                    break;
                case "build" :
                    this.build = value;
                    break;
                case "buff-in" :
                    try {
                        this.buffIn = Integer.parseInt(value);
                    } catch (NumberFormatException nfe) {
                        this.buffIn = 0;
                    }
                    break;
            }
        }
    }
}
