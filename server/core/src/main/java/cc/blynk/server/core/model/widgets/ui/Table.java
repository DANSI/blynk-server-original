package cc.blynk.server.core.model.widgets.ui;

import cc.blynk.server.core.model.enums.PinType;
import cc.blynk.server.core.model.widgets.OnePinWidget;
import cc.blynk.utils.ParseUtil;
import cc.blynk.utils.structure.TableLimitedQueue;

import static cc.blynk.utils.StringUtils.BODY_SEPARATOR_STRING;


/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 28.03.16.
 */
public class Table extends OnePinWidget {

    public Column[] columns;

    public TableLimitedQueue<Row> rows = new TableLimitedQueue<>();

    public int currentRowIndex;

    public boolean isReoderingAllowed;

    public boolean isClickableRows;

    @Override
    public boolean updateIfSame(byte pin, PinType type, String value) {
        if (isSame(pin, type)) {
            String[] values = value.split(BODY_SEPARATOR_STRING);
            if (values.length > 0) {
                String tableCommand = values[0];
                switch (tableCommand) {
                    case "clr" :
                        rows.clear();
                        currentRowIndex = 0;
                        break;
                    case "add" :
                        int id = ParseUtil.parseInt(values[1]);
                        String rowName = values[2];
                        String rowValue = values[3];
                        rows.add(new Table.Row(id, rowName, rowValue));
                        break;
                    case "pick" :
                        currentRowIndex = ParseUtil.parseInt(values[1]);
                        break;
                    case "select" :
                        selectRow(values[1], true);
                        break;
                    case "deselect" :
                        selectRow(values[1], false);
                        break;
                    case "order" :
                        int oldIndex = ParseUtil.parseInt(values[1]);
                        int newIndex = ParseUtil.parseInt(values[2]);
                        rows.order(oldIndex, newIndex);
                        break;
                }
            }
            return true;
        }
        return false;
    }

    private void selectRow(String indexString, boolean select) {
        int index = ParseUtil.parseInt(indexString);
        Row row = rows.get(index);
        if (row != null) {
            row.isSelected = select;
        }
    }

    @Override
    public String getModeType() {
        return "out";
    }

    @Override
    public int getPrice() {
        return 800;
    }

    static class Column {

        public String name;

    }

    static class Row {

        public int id;

        public String name;

        public String value;

        public boolean isSelected;

        public Row() {
        }

        public Row(int id, String name, String value) {
            this.id = id;
            this.name = name;
            this.value = value;
            this.isSelected = true;
        }
    }

}
