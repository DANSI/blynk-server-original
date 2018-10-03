package cc.blynk.server.core.model.device;

//utility class to make fields of HardwareInfo final, used instead of hashmap
public final class HardwareInfoPrivate {

    public String version;
    public String blynkVersion;
    public String boardType;
    public String cpuType;
    public String connectionType;
    public String templateId;
    public String build;
    public int heartbeatInterval;
    public int buffIn;

    public HardwareInfoPrivate(String[] info) {
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
                this.blynkVersion = value;
                break;
            case "fw" :
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
