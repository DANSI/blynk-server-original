package cc.blynk.server.application.handlers.main.logic.graph;

import cc.blynk.server.core.model.enums.PinType;

import java.nio.file.Path;
import java.util.List;

public class FileLink {

    private final Path path;

    private final String dashName;

    private final PinType pinType;

    private final byte pin;

    public FileLink(Path path, String dashName, PinType pinType, byte pin) {
        this.path = path;
        this.dashName = dashName;
        this.pinType = pinType;
        this.pin = pin;
    }

    public static String makeBody(String downLoadUrl, List<FileLink> fileUrls) {
        var sb = new StringBuilder();
        sb.append("<html><body>");
        for (FileLink link : fileUrls) {
            sb.append(link.makeAHRef(downLoadUrl)).append("<br>");
        }
        return sb.append("</body></html>").toString();
    }

    private String makeAHRef(String csvDownloadUrl) {
        return "<a href=\"" + csvDownloadUrl + path + "\">" + dashName + " " + pinType.pintTypeChar + pin + "</a>";
    }
}
