package cc.blynk.server.core.model.widgets.ui;

import cc.blynk.utils.JsonParser;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 8/20/16.
 */
public class TableSerializationTest {

    @Test
    public void testTableJson() throws Exception {
        Table table = new Table();
        table.columns = new Table.Column[3];
        table.columns[0] = new Table.Column();
        table.columns[0].name = "indicator";
        table.columns[1] = new Table.Column();
        table.columns[1].name = "name";
        table.columns[2] = new Table.Column();
        table.columns[2].name = "value";

        table.rows = new Table.Row[1];
        table.rows[0] = new Table.Row();
        table.rows[0].name = "Adskiy trash";
        table.rows[0].value = "6:33";
        table.rows[0].isSelected = false;
        table.rows[0].clientId = 1;
        table.rows[0].hardwareId = 1;

        String json = JsonParser.mapper.writeValueAsString(table);
        assertEquals("{\"type\":\"TABLE\",\"id\":0,\"x\":0,\"y\":0,\"color\":0,\"width\":0,\"height\":0,\"tabId\":0,\"pin\":-1,\"pwmMode\":false,\"rangeMappingOn\":false,\"min\":0,\"max\":0,\"columns\":[{\"name\":\"indicator\"},{\"name\":\"name\"},{\"name\":\"value\"}],\"rows\":[{\"isSelected\":false,\"name\":\"Adskiy trash\",\"value\":\"6:33\",\"clientId\":1,\"hardwareId\":1}],\"isReoderingAllowed\":false,\"isClickableRows\":false}", json);
    }

}
