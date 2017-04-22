package cc.blynk.server.core.model;

import cc.blynk.server.core.model.enums.Theme;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 22.04.17.
 */
public class DashboardSettings {

    public String name;

    public boolean isShared;

    public Theme theme = Theme.Blynk;

    public boolean keepScreenOn;

    public boolean isAppConnectedOn;

}
