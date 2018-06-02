package cc.blynk.server.application.handlers.main.logic.graph.links;

import java.nio.file.Path;

public class ReportFileLink {

    private final Path path;

    private final String name;

    public ReportFileLink(Path path, String name) {
        this.path = path;
        this.name = name;
    }

    public String makeBody(String downLoadUrl) {
        return "<html><body>" +
                makeAHRef(downLoadUrl) + "<br>" +
                "</body></html>";
    }

    private String makeAHRef(String csvDownloadUrl) {
        return "<a href=\"" + csvDownloadUrl + path + "\">" + name + "</a>";
    }
}
