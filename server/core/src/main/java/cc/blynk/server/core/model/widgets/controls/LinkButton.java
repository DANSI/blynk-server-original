package cc.blynk.server.core.model.widgets.controls;

import cc.blynk.server.core.model.widgets.NoPinWidget;
import cc.blynk.server.core.model.widgets.outputs.graph.FontSize;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 21.03.15.
 */
public class LinkButton extends NoPinWidget {

    public String url;

    public ButtonState onButtonState;

    public ButtonState offButtonState;

    public FontSize fontSize;

    public Edge edge;

    public ButtonStyle buttonStyle;

    public boolean lockSize;

    public boolean showAddressBar;

    public boolean showNavigationBar;

    public boolean allowInBrowser;

    @Override
    public int getPrice() {
        return 500;
    }

}
