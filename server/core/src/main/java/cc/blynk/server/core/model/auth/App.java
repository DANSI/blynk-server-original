package cc.blynk.server.core.model.auth;

import cc.blynk.server.core.model.enums.ProvisionType;
import cc.blynk.server.core.model.enums.Theme;
import cc.blynk.server.core.protocol.exceptions.NotAllowedException;
import cc.blynk.utils.JsonParser;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 5/10/17.
 */
public class App {

    public String id;

    public volatile Theme theme;

    public volatile ProvisionType provisionType;

    public volatile int color;

    public volatile boolean isMultiFace;

    public volatile String name;

    public volatile String icon;

    public volatile int[] projectIds;

    public App() {
    }

    public void update(App newApp) {
        this.theme = newApp.theme;
        this.provisionType = newApp.provisionType;
        this.color = newApp.color;
        this.isMultiFace = newApp.isMultiFace;
        this.name = newApp.name;
        this.icon = newApp.icon;
        this.projectIds = newApp.projectIds;
    }

    public void validate() {
        if (theme == null || provisionType == null || name == null ||
                name.isEmpty() || projectIds == null) {
            throw new NotAllowedException("App is not valid.");
        }
    }

    @Override
    public String toString() {
        return JsonParser.toJson(this);
    }
}
