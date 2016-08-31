package cc.blynk.server.core.model.widgets.others;

import cc.blynk.server.core.model.widgets.OnePinWidget;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 28.03.16.
 */
public class Player extends OnePinWidget {

    @Override
    public String getModeType() {
        return "out";
    }

    @Override
    public int getPrice() {
        return 400;
    }

}
