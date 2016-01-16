package cc.blynk.common.utils;

import cc.blynk.utils.StringUtils;
import org.openjdk.jmh.annotations.*;

import java.util.concurrent.TimeUnit;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 16.01.16.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Thread)
@Fork(1)
@Warmup(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
public class StringUtilPerfTest {

     /*
     * Should your benchmark require returning multiple results, you have to
     * consider two options (detailed below).
     *
     * NOTE: If you are only producing a single result, it is more readable to
     * use the implicit return, as in JMHSample_08_DeadCode. Do not make your benchmark
     * code less readable with explicit Blackholes!
     */

    @Param({"aw\01\02", "aw\0100\0200", "aw\010\0  dsfdsfdsfdsfdsfdsfdsfdsfd gfdsgdfg dfg dfg dfsgdf gdfs gdfsg dfsg dfsg dfsg dfsg dfsg dfs gdfsgsfds"})
    public String s;

    /*
     * Baseline measurement: how much single Math.log costs.
     */

    @Benchmark
    @CompilerControl(CompilerControl.Mode.INLINE)
    public String[] split() {
        return s.split("\0");
    }

    @Benchmark
    @CompilerControl(CompilerControl.Mode.INLINE)
    public String[] ownfSplit1() {
        return StringUtils.split3(s);
    }


}
