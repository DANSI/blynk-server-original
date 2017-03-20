package cc.blynk.server.core.model.publishing;

import cc.blynk.server.core.model.enums.ProvisionType;
import cc.blynk.server.core.model.enums.Theme;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 20.03.17.
 */
public class Publishing {

    public Theme theme;

    public ProvisionType provisionType;

    public Publishing() {
    }

    public Publishing(Theme theme, ProvisionType provisionType) {
        this.theme = theme;
        this.provisionType = provisionType;
    }
}
