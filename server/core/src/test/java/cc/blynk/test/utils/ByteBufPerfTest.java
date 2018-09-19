package cc.blynk.test.utils;

import cc.blynk.server.core.model.enums.PinType;
import cc.blynk.server.core.protocol.enums.Command;
import cc.blynk.server.core.protocol.model.messages.StringMessage;
import cc.blynk.server.core.protocol.model.messages.common.HardwareMessage;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.util.CharsetUtil;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.BenchmarkParams;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

import static cc.blynk.server.core.protocol.model.messages.MessageFactory.produce;
import static cc.blynk.utils.StringUtils.split3;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 16.01.16.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Thread)
@Fork(1)
@Warmup(iterations = 10, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 10, time = 1, timeUnit = TimeUnit.SECONDS)
public class ByteBufPerfTest {

     /*
     * Should your benchmark require returning multiple results, you have to
     * consider two options (detailed below).
     *
     * NOTE: If you are only producing a single result, it is more readable to
     * use the implicit return, as in JMHSample_08_DeadCode. Do not make your benchmark
     * code less readable with explicit Blackholes!
     */

    public ByteBuf in;


    @Setup
    public void setup(BenchmarkParams params) {
        String msg = "vw 1 111.3323_C".replace(" ", "\0");
        byte[] msgBytes = msg.getBytes(StandardCharsets.UTF_8);
        in = ByteBufAllocator.DEFAULT.buffer(5 + msgBytes.length);
        in.writeByte(Command.HARDWARE);
        in.writeShort(1);
        in.writeShort(msg.length());
        in.writeBytes(msgBytes);
    }

    @Benchmark
    public String optimizedFlow() {
        short command = in.readUnsignedByte();
        int messageId = in.readUnsignedShort();
        int codeOrLength = in.readUnsignedShort();

        PinType pinType = PinType.getPinType((char) in.readByte());
        in.readByte();
        in.readByte();

        byte initByte = in.readByte();
        int length = 1;

        byte pin = (byte) (initByte - 48);
        initByte = in.readByte();

        if (initByte != 0) {
            length++;
            pin *= 10;
            pin += (byte) (initByte - 48);
            initByte = in.readByte();

            if (initByte != 0) {
                length++;
                pin *= 10;
                pin += (byte) (initByte - 48);
            }
        }

        String value = (String) in.readCharSequence(codeOrLength - (3 + length + 1), CharsetUtil.UTF_8);

        HardwareMessage hardwareMessage = new HardwareMessage(messageId, value);

        if (pinType != PinType.VIRTUAL || pin != 1) {
            throw new RuntimeException();
        }

        in.resetReaderIndex();

        return hardwareMessage.body;
    }

    @Benchmark
    public String mainFlow() {
        short command = in.readUnsignedByte();
        int messageId = in.readUnsignedShort();
        int codeOrLength = in.readUnsignedShort();
        StringMessage message = (StringMessage)
                produce(messageId, command, (String) in.readCharSequence(codeOrLength, CharsetUtil.UTF_8));

        String body = message.body;
        String[] splitBody = split3(body);
        PinType pinType = PinType.getPinType(splitBody[0].charAt(0));
        byte pin = Byte.parseByte(splitBody[1]);
        String value = splitBody[2];

        if (pinType != PinType.VIRTUAL || pin != 1) {
            throw new RuntimeException();
        }

        in.resetReaderIndex();

        return value;
    }

    public static void main(String[] args) {
        ByteBuf in;
        String msg = "vw 1 111.3323_C".replace(" ", "\0");
        byte[] msgBytes = msg.getBytes(StandardCharsets.UTF_8);
        in = ByteBufAllocator.DEFAULT.buffer(5 + msgBytes.length);
        in.writeByte(Command.HARDWARE);
        in.writeShort(1);
        in.writeShort(msg.length());
        in.writeBytes(msgBytes);

        short command = in.readUnsignedByte();
        int messageId = in.readUnsignedShort();
        int codeOrLength = in.readUnsignedShort();

        PinType pinType = PinType.getPinType((char) in.readByte());
        char actionByte = (char) in.readByte();
        if (actionByte != 'w') {
            throw new RuntimeException();
        }
        byte separator = in.readByte();
        if (separator != 0) {
            throw new RuntimeException();
        }

        //int currentPosition = in.readerIndex();
        //int length = in.bytesBefore((byte) 0);
        //byte pin = Byte.parseByte(in.toString(currentPosition, length, StandardCharsets.US_ASCII));
        byte initByte = in.readByte();
        int length = 1;

        byte pin = (byte) (initByte - 48);
        initByte = in.readByte();

        if (initByte != 0) {
            length++;
            pin *= 10;
            pin += (byte) (initByte - 48);
            initByte = in.readByte();

            if (initByte != 0) {
                length++;
                pin *= 10;
                pin += (byte) (initByte - 48);
            }
        }

        String value = (String) in.readCharSequence(codeOrLength - (3 + length + 1), CharsetUtil.UTF_8);

        HardwareMessage hardwareMessage = new HardwareMessage(messageId, value);

        if (pinType != PinType.VIRTUAL || pin != 1 || !hardwareMessage.body.equals("111.3323_C")) {
            throw new RuntimeException();
        }

    }

}
