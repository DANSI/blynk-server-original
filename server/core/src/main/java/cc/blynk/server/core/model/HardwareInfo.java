package cc.blynk.server.core.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 18.05.16.
 */
public class HardwareInfo {

    public String version;

    public String boardType;

    public String cpuType;

    public String connectionType;

    public String buildDate;

    public int heartbeatInterval;

    @JsonCreator
    public HardwareInfo(@JsonProperty("version") String version,
                        @JsonProperty("boardType") String boardType,
                        @JsonProperty("cpuType") String cpuType,
                        @JsonProperty("connectionType") String connectionType,
                        @JsonProperty("buildDate") String buildDate,
                        @JsonProperty("heartbeatInterval") int heartbeatInterval) {
        this.version = version;
        this.boardType = boardType;
        this.cpuType = cpuType;
        this.connectionType = connectionType;
        this.buildDate = buildDate;
        this.heartbeatInterval = heartbeatInterval;
    }

    public HardwareInfo(String[] info) {
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
            case "build" :
                this.buildDate = value;
                break;
        }
    }

}
