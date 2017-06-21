package cc.blynk.utils;

import cc.blynk.server.core.model.DashBoard;
import cc.blynk.server.core.model.HardwareInfo;
import cc.blynk.server.core.model.device.ConnectionType;
import cc.blynk.server.core.model.device.Device;
import cc.blynk.server.core.model.widgets.Widget;
import cc.blynk.server.core.model.widgets.controls.Button;
import org.junit.Assert;
import org.openjdk.jmh.annotations.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;
import java.util.zip.DeflaterOutputStream;

import static cc.blynk.utils.JsonParser.gzipDash;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 16.01.16.
 */
@BenchmarkMode(Mode.Throughput)
@State(Scope.Thread)
@Fork(1)
@Warmup(iterations = 10, time = 1, timeUnit = TimeUnit.SECONDS, batchSize = 1000)
@Measurement(iterations = 20, time = 1, timeUnit = TimeUnit.SECONDS, batchSize = 1000)
public class DashSerializationPerfTest {

     /*
     * Should your benchmark require returning multiple results, you have to
     * consider two options (detailed below).
     *
     * NOTE: If you are only producing a single result, it is more readable to
     * use the implicit return, as in JMHSample_08_DeadCode. Do not make your benchmark
     * code less readable with explicit Blackholes!
     */

    public DashBoard dash;

    @Setup
    public void setup()  throws Exception {
        dash = new DashBoard();
        dash.id = 1;
        dash.name = "My new Dashboard";
        dash.hardwareInfo = new HardwareInfo(null, null, null, null, null, 0);
        dash.devices = new Device[10];
        for (int i = 0; i < 10; i++) {
            dash.devices[i] = new Device(1, "sdaasdas", "12321321321321321321321321", "@!3213", ConnectionType.ETHERNET);
        }
        dash.widgets = new Widget[40];
        for (int i = 0; i < 40; i++) {
            dash.widgets[i] = new Button();
        }

        Assert.assertArrayEquals(compress(dash.toString()), gzipDash(dash));

    }

    @Benchmark
    public byte[] oldMethod() throws Exception {
        return compress(dash.toString());
    }

    @Benchmark
    public byte[] newMethod() throws Exception {
        return gzipDash(dash);
    }

    public static byte[] compress(String value) throws IOException {
        byte[] stringData = value.getBytes(StandardCharsets.UTF_8);
        ByteArrayOutputStream baos = new ByteArrayOutputStream(stringData.length);

        try (OutputStream out = new DeflaterOutputStream(baos)) {
            out.write(stringData);
        }

        return baos.toByteArray();
    }


}
