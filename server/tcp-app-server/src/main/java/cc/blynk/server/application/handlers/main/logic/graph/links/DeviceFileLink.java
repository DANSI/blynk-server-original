package cc.blynk.server.application.handlers.main.logic.graph.links;

import cc.blynk.server.core.model.enums.PinType;

import java.nio.file.Path;
import java.util.List;

public class DeviceFileLink {

    private final Path path;

    private final String name;

    private final PinType pinType;

    private final short pin;

    public DeviceFileLink(Path path, String name, PinType pinType, short pin) {
        this.path = path.getFileName();
        this.name = name;
        this.pinType = pinType;
        this.pin = pin;
    }

    public static String makeBody(String downLoadUrl, List<DeviceFileLink> fileUrls) {
        var sb = new StringBuilder();
        sb.append("<html><body>");
        for (DeviceFileLink link : fileUrls) {
            sb.append(link.makeAHRef(downLoadUrl)).append("<br>");
        }
        return sb.append("</body></html>").toString();
    }

    private String makeAHRef(String csvDownloadUrl) {
        return "<a href=\"" + csvDownloadUrl + path + "\">" + name + " " + pinType.pintTypeChar + pin + "</a>";
    }
}
