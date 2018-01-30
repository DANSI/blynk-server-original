package cc.blynk.server.core.model.auth;

import cc.blynk.server.core.model.enums.ProvisionType;
import cc.blynk.server.core.model.enums.Theme;
import cc.blynk.server.core.model.serialization.JsonParser;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

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

    @JsonCreator
    public App(@JsonProperty("id") String id,
               @JsonProperty("theme") Theme theme,
               @JsonProperty("provisionType") ProvisionType provisionType,
               @JsonProperty("color") int color,
               @JsonProperty("isMultiFace") boolean isMultiFace,
               @JsonProperty("name") String name,
               @JsonProperty("icon") String icon,
               @JsonProperty("projectIds") int[] projectIds) {
        this.id = id;
        this.theme = theme;
        this.provisionType = provisionType;
        this.color = color;
        this.isMultiFace = isMultiFace;
        this.name = name;
        this.icon = icon;
        this.projectIds = projectIds;
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

    public boolean isNotValid() {
        return theme == null || provisionType == null || name == null
                || name.isEmpty() || projectIds == null;
    }

    @Override
    public String toString() {
        return JsonParser.toJson(this);
    }
}
