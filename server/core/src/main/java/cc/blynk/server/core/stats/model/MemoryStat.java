package cc.blynk.server.core.stats.model;

import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.ByteBufAllocatorMetric;
import io.netty.buffer.ByteBufAllocatorMetricProvider;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 5/18/17.
 */
public class MemoryStat {

    public final long heapBytes;

    public final long directBytes;

    public MemoryStat(ByteBufAllocator byteBufAllocator) {
        long directMemory = 0;
        long heapMemory = 0;

        if (byteBufAllocator instanceof ByteBufAllocatorMetricProvider) {
            ByteBufAllocatorMetric metric = ((ByteBufAllocatorMetricProvider) byteBufAllocator).metric();
            directMemory = metric.usedDirectMemory();
            heapMemory = metric.usedHeapMemory();
        }

        this.directBytes = directMemory;
        this.heapBytes = heapMemory;
    }
}
