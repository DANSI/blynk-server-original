package cc.blynk.server.core.model.widgets.others;

import cc.blynk.server.core.model.widgets.NoPinWidget;
import cc.blynk.server.core.model.widgets.outputs.graph.FontSize;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 31.05.19.
 */
public class TextWidget extends NoPinWidget {

    public FontSize textSize = FontSize.AUTO;

    public String text;

    @Override
    public int getPrice() {
        return 100;
    }

}
