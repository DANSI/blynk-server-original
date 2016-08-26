package cc.blynk.server.core.model.widgets.others;

import cc.blynk.server.core.model.widgets.OnePinWidget;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 28.03.16.
 */
public class Table extends OnePinWidget {

    public Column[] columns;

    public Row[] rows;

    public boolean isReoderingAllowed;

    public boolean isClickableRows;

    @Override
    public String getModeType() {
        return "out";
    }

    @Override
    public int getPrice() {
        return 0;
    }

    static class Column {

        public String name;

    }

    static class Row {

        public boolean isSelected;

        public String name;

        public String value;

        public int clientId;

        public int hardwareId;
    }

}
