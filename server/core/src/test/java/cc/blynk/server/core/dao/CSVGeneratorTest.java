package cc.blynk.server.core.dao;

import cc.blynk.server.core.model.auth.User;
import cc.blynk.utils.AppNameUtil;
import org.junit.Test;

import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 29.06.17.
 */
public class CSVGeneratorTest {

    private CSVGenerator csvGenerator = new CSVGenerator(new ReportingDao("/tmp", true));

    @Test
    public void generateCSV() throws Exception {
        User user  = new User();
        user.email = "test@blynk.cc";
        user.appName = AppNameUtil.BLYNK;

        Path path = Paths.get("/home/doom369/hourly_data.csv.gz");

        final ByteBuffer buf = ByteBuffer.allocate(2 * 16);
        buf.putDouble(1).putLong(1);
        buf.putDouble(2).putLong(2);
        buf.flip();

        //CSVGenerator.makeGzippedCSVFile(buf, path);
    }

}
