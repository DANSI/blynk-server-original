package cc.blynk.server.core.model;

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

    //used for json.
    public HardwareInfo() {
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        HardwareInfo that = (HardwareInfo) o;

        if (heartbeatInterval != that.heartbeatInterval) return false;
        if (version != null ? !version.equals(that.version) : that.version != null) return false;
        if (boardType != null ? !boardType.equals(that.boardType) : that.boardType != null) return false;
        if (cpuType != null ? !cpuType.equals(that.cpuType) : that.cpuType != null) return false;
        if (connectionType != null ? !connectionType.equals(that.connectionType) : that.connectionType != null)
            return false;
        return !(buildDate != null ? !buildDate.equals(that.buildDate) : that.buildDate != null);

    }

    @Override
    public int hashCode() {
        int result = version != null ? version.hashCode() : 0;
        result = 31 * result + (boardType != null ? boardType.hashCode() : 0);
        result = 31 * result + (cpuType != null ? cpuType.hashCode() : 0);
        result = 31 * result + (connectionType != null ? connectionType.hashCode() : 0);
        result = 31 * result + (buildDate != null ? buildDate.hashCode() : 0);
        result = 31 * result + heartbeatInterval;
        return result;
    }
}
