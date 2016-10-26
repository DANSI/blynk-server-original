package cc.blynk.server.core.model.auth;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.PooledByteBufAllocator;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.BenchmarkParams;

import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Thread)
@Fork(1)
@Warmup(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
public class StringToByteBufPerfTest {

    public PooledByteBufAllocator DEFAULT;

    @Param({"", "small", "This is test String!", "ddddddd dddddddddddddd ddddddddddddd ddddddddddddddddddddddddddddddddd"})
    public String data;

    @Setup
    public void setup(BenchmarkParams params) {
        DEFAULT = new PooledByteBufAllocator(true);
    }

    @Benchmark
    public ByteBuf javaWay() {
        ByteBuf byteBuf = null;
        try {
            byte[] bytes = data.getBytes(StandardCharsets.UTF_8);
            byteBuf = DEFAULT.buffer(bytes.length).writeBytes(bytes);
            return byteBuf;
        } finally {
            if (byteBuf != null) {
                byteBuf.release();
            }
        }
    }

    @Benchmark
    public ByteBuf nettyWay() {
        ByteBuf byteBuf = null;
        try {
            byteBuf = ByteBufUtil.encodeString(DEFAULT, CharBuffer.wrap(data), StandardCharsets.UTF_8, 0);
            return byteBuf;
        } finally {
            if (byteBuf != null) {
                byteBuf.release();
            }
        }
    }

    @Benchmark
    public ByteBuf nettyWayWriteUtf8() {
        ByteBuf byteBuf = null;
        try {
            byteBuf = ByteBufUtil.writeUtf8(DEFAULT, data);
            return byteBuf;
        } finally {
            if (byteBuf != null) {
                byteBuf.release();
            }
        }
    }

}
