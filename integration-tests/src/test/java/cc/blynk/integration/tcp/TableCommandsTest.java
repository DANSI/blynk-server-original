package cc.blynk.integration.tcp;

import cc.blynk.integration.SingleServerInstancePerTest;
import cc.blynk.server.core.model.Profile;
import cc.blynk.server.core.model.enums.PinType;
import cc.blynk.server.core.model.widgets.ui.table.Row;
import cc.blynk.server.core.model.widgets.ui.table.Table;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import static cc.blynk.integration.TestUtil.b;
import static cc.blynk.integration.TestUtil.ok;
import static cc.blynk.server.core.protocol.enums.Command.HARDWARE;
import static cc.blynk.server.core.protocol.model.messages.MessageFactory.produce;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 7/09/2016.
 *
 */
@RunWith(MockitoJUnitRunner.class)
public class TableCommandsTest extends SingleServerInstancePerTest {

    @Test
    public void testAllTableCommands() throws Exception {
        Table table = new Table();
        table.pin = 123;
        table.pinType = PinType.VIRTUAL;
        table.isClickableRows = true;
        table.isReoderingAllowed = true;
        table.height = 2;
        table.width = 2;

        clientPair.appClient.createWidget(1, table);
        clientPair.appClient.verifyResult(ok(1));

        clientPair.hardwareClient.send("hardware vw 123 clr");
        verify(clientPair.hardwareClient.responseMock, timeout(500).times(0)).channelRead(any(), any());
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(produce(1, HARDWARE, b("1-0 vw 123 clr"))));

        clientPair.hardwareClient.send("hardware vw 123 add 0 Row0 row0");
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(produce(2, HARDWARE, b("1-0 vw 123 add 0 Row0 row0"))));

        table = loadTable();
        Row row;

        assertNotNull(table);
        assertNotNull(table.rows);
        assertEquals(1, table.rows.size());
        row = table.rows.poll();
        assertNotNull(row);
        assertEquals(0, row.id);
        assertEquals("Row0", row.name);
        assertEquals("row0", row.value);
        assertTrue(row.isSelected);
        assertEquals(0, table.currentRowIndex);

        clientPair.hardwareClient.send("hardware vw 123 pick 2");
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(produce(3, HARDWARE, b("1-0 vw 123 pick 2"))));

        table = loadTable();
        assertNotNull(table);
        assertNotNull(table.rows);
        assertEquals(1, table.rows.size());
        row = table.rows.poll();
        assertNotNull(row);
        assertEquals(2, table.currentRowIndex);

        clientPair.hardwareClient.send("hardware vw 123 add 1 Row1 row1");
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(produce(4, HARDWARE, b("1-0 vw 123 add 1 Row1 row1"))));
        clientPair.hardwareClient.send("hardware vw 123 add 2 Row2 row2");
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(produce(5, HARDWARE, b("1-0 vw 123 add 2 Row2 row2"))));
        clientPair.hardwareClient.send("hardware vw 123 pick 2");
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(produce(6, HARDWARE, b("1-0 vw 123 pick 2"))));

        table = loadTable();
        assertNotNull(table);
        assertNotNull(table.rows);
        assertEquals(3, table.rows.size());
        row = table.rows.poll();
        assertNotNull(row);
        assertEquals(2, table.currentRowIndex);

        clientPair.hardwareClient.send("hardware vw 123 deselect 1");
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(produce(7, HARDWARE, b("1-0 vw 123 deselect 1"))));

        table = loadTable();
        assertNotNull(table);
        assertNotNull(table.rows);
        assertEquals(3, table.rows.size());
        table.rows.poll();
        row = table.rows.poll();
        assertNotNull(row);
        assertFalse(row.isSelected);

        clientPair.hardwareClient.send("hardware vw 123 select 1");
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(produce(8, HARDWARE, b("1-0 vw 123 select 1"))));

        table = loadTable();
        assertNotNull(table);
        assertNotNull(table.rows);
        assertEquals(3, table.rows.size());
        table.rows.poll();
        row = table.rows.poll();
        assertNotNull(row);
        assertTrue(row.isSelected);

        /*
        clientPair.hardwareClient.send("hardware vw 123 order 0 2");
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(produce(9, HARDWARE, b("1-0 vw 123 order 0 2"))));

        table = loadTable();
        assertNotNull(table);
        assertNotNull(table.rows);
        assertEquals(3, table.rows.size());

        assertEquals(1, table.rows.poll().id);
        assertEquals(2, table.rows.poll().id);
        assertEquals(0, table.rows.poll().id);
        */

        clientPair.hardwareClient.send("hardware vw 123 clr");
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(produce(9, HARDWARE, b("1-0 vw 123 clr"))));
        table = loadTable();
        assertNotNull(table);
        assertNotNull(table.rows);
        assertEquals(0, table.rows.size());
    }

    @Test
    public void testTableUpdateExistingRow() throws Exception {
        Table table = new Table();
        table.pin = 123;
        table.pinType = PinType.VIRTUAL;
        table.isClickableRows = true;
        table.isReoderingAllowed = true;
        table.height = 2;
        table.width = 2;

        clientPair.appClient.createWidget(1, table);
        clientPair.appClient.verifyResult(ok(1));

        clientPair.hardwareClient.send("hardware vw 123 clr");
        verify(clientPair.hardwareClient.responseMock, timeout(500).times(0)).channelRead(any(), any());
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(produce(1, HARDWARE, b("1-0 vw 123 clr"))));

        clientPair.hardwareClient.send("hardware vw 123 add 0 Row0 row0");
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(produce(2, HARDWARE, b("1-0 vw 123 add 0 Row0 row0"))));

        table = loadTable();
        Row row;

        assertNotNull(table);
        assertNotNull(table.rows);
        assertEquals(1, table.rows.size());
        row = table.rows.poll();
        assertNotNull(row);
        assertEquals(0, row.id);
        assertEquals("Row0", row.name);
        assertEquals("row0", row.value);
        assertTrue(row.isSelected);
        assertEquals(0, table.currentRowIndex);

        clientPair.hardwareClient.send("hardware vw 123 update 0 Row0Updated row0Updated");
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(produce(3, HARDWARE, b("1-0 vw 123 update 0 Row0Updated row0Updated"))));

        table = loadTable();

        assertNotNull(table);
        assertNotNull(table.rows);
        assertEquals(1, table.rows.size());
        row = table.rows.poll();
        assertNotNull(row);
        assertEquals(0, row.id);
        assertEquals("Row0Updated", row.name);
        assertEquals("row0Updated", row.value);
        assertTrue(row.isSelected);
        assertEquals(0, table.currentRowIndex);
    }

    @Test
    public void testTableRowLimit() throws Exception {
        Table table = new Table();
        table.pin = 123;
        table.pinType = PinType.VIRTUAL;
        table.isClickableRows = true;
        table.isReoderingAllowed = true;
        table.width = 2;
        table.height = 2;

        clientPair.appClient.createWidget(1, table);
        clientPair.appClient.verifyResult(ok(1));

        clientPair.hardwareClient.send("hardware vw 123 clr");
        verify(clientPair.hardwareClient.responseMock, timeout(500).times(0)).channelRead(any(), any());
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(produce(1, HARDWARE, b("1-0 vw 123 clr"))));

        for (int i = 1; i <= 101; i++) {
            String cmd = "vw 123 add " + i + " Row0 row0";
            clientPair.hardwareClient.send("hardware " + cmd);
            verify(clientPair.appClient.responseMock, timeout(700)).channelRead(any(), eq(produce(i + 1, HARDWARE, b("1-0 " + cmd))));
        }


        table = loadTable();
        Row row;

        assertNotNull(table);
        assertNotNull(table.rows);
        assertEquals(100, table.rows.size());
        for (int i = 2; i <= 101; i++) {
            row = table.rows.poll();
            assertNotNull(row);
            assertEquals(i, row.id);
        }
    }

    @Test
    public void testTableAcceptsOnlyUniqueIds() throws Exception {
        Table table = new Table();
        table.pin = 123;
        table.pinType = PinType.VIRTUAL;
        table.isClickableRows = true;
        table.isReoderingAllowed = true;
        table.width = 2;
        table.height = 2;

        clientPair.appClient.createWidget(1, table);
        clientPair.appClient.verifyResult(ok(1));

        clientPair.hardwareClient.send("hardware vw 123 clr");
        verify(clientPair.hardwareClient.responseMock, timeout(500).times(0)).channelRead(any(), any());
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(produce(1, HARDWARE, b("1-0 vw 123 clr"))));

        clientPair.hardwareClient.send("hardware vw 123 add 0 row0 val0");
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(produce(2, HARDWARE, b("1-0 vw 123 add 0 row0 val0"))));

        clientPair.hardwareClient.send("hardware vw 123 add 0 row1 val1");
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(produce(3, HARDWARE, b("1-0 vw 123 add 0 row1 val1"))));

        table = loadTable();

        assertEquals(1, table.rows.size());
        assertEquals("row1", table.rows.peek().name);
        assertEquals("val1", table.rows.peek().value);
    }

    private Table loadTable() throws Exception {
        clientPair.appClient.reset();
        clientPair.appClient.send("loadProfileGzipped");
        Profile profile = clientPair.appClient.parseProfile(1);
        return (Table) profile.dashBoards[0].findWidgetByPin(0, (short) 123, PinType.VIRTUAL);
    }

}
