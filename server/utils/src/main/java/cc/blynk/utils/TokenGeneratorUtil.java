package cc.blynk.utils;

import java.util.UUID;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 12.05.16.
 */
public final class TokenGeneratorUtil {

    private TokenGeneratorUtil() {
    }

    public static String generateNewToken() {
        return fastToString(UUID.randomUUID());
    }

    private static final int UUID_STRING_LENGTH = 32;

    private static final char[] HEX_DIGITS = new char[] {
            '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'
    };

    // based on
    // https://github.com/jchambers/fast-uuid
    // faster implementation of toString.
    /**
     * Returns a string representation of the given UUID. The returned string is formatted as described in
     * {@link UUID#toString()}.
     *
     * @param uuid the UUID to represent as a string
     *
     * @return a string representation of the given UUID
     */
    public static String fastToString(final UUID uuid) {
        long mostSignificantBits = uuid.getMostSignificantBits();
        long leastSignificantBits = uuid.getLeastSignificantBits();

        char[] uuidChars = new char[UUID_STRING_LENGTH];

        uuidChars[0]  = HEX_DIGITS[(int) ((mostSignificantBits & 0xf000000000000000L) >>> 60)];
        uuidChars[1]  = HEX_DIGITS[(int) ((mostSignificantBits & 0x0f00000000000000L) >>> 56)];
        uuidChars[2]  = HEX_DIGITS[(int) ((mostSignificantBits & 0x00f0000000000000L) >>> 52)];
        uuidChars[3]  = HEX_DIGITS[(int) ((mostSignificantBits & 0x000f000000000000L) >>> 48)];
        uuidChars[4]  = HEX_DIGITS[(int) ((mostSignificantBits & 0x0000f00000000000L) >>> 44)];
        uuidChars[5]  = HEX_DIGITS[(int) ((mostSignificantBits & 0x00000f0000000000L) >>> 40)];
        uuidChars[6]  = HEX_DIGITS[(int) ((mostSignificantBits & 0x000000f000000000L) >>> 36)];
        uuidChars[7]  = HEX_DIGITS[(int) ((mostSignificantBits & 0x0000000f00000000L) >>> 32)];
        uuidChars[8]  = HEX_DIGITS[(int) ((mostSignificantBits & 0x00000000f0000000L) >>> 28)];
        uuidChars[9] = HEX_DIGITS[(int) ((mostSignificantBits & 0x000000000f000000L) >>> 24)];
        uuidChars[10] = HEX_DIGITS[(int) ((mostSignificantBits & 0x0000000000f00000L) >>> 20)];
        uuidChars[11] = HEX_DIGITS[(int) ((mostSignificantBits & 0x00000000000f0000L) >>> 16)];
        uuidChars[12] = HEX_DIGITS[(int) ((mostSignificantBits & 0x000000000000f000L) >>> 12)];
        uuidChars[13] = HEX_DIGITS[(int) ((mostSignificantBits & 0x0000000000000f00L) >>> 8)];
        uuidChars[14] = HEX_DIGITS[(int) ((mostSignificantBits & 0x00000000000000f0L) >>> 4)];
        uuidChars[15] = HEX_DIGITS[(int)  (mostSignificantBits & 0x000000000000000fL)];
        uuidChars[16]  = HEX_DIGITS[(int) ((leastSignificantBits & 0xf000000000000000L) >>> 60)];
        uuidChars[17]  = HEX_DIGITS[(int) ((leastSignificantBits & 0x0f00000000000000L) >>> 56)];
        uuidChars[18]  = HEX_DIGITS[(int) ((leastSignificantBits & 0x00f0000000000000L) >>> 52)];
        uuidChars[19]  = HEX_DIGITS[(int) ((leastSignificantBits & 0x000f000000000000L) >>> 48)];
        uuidChars[20]  = HEX_DIGITS[(int) ((leastSignificantBits & 0x0000f00000000000L) >>> 44)];
        uuidChars[21]  = HEX_DIGITS[(int) ((leastSignificantBits & 0x00000f0000000000L) >>> 40)];
        uuidChars[22]  = HEX_DIGITS[(int) ((leastSignificantBits & 0x000000f000000000L) >>> 36)];
        uuidChars[23]  = HEX_DIGITS[(int) ((leastSignificantBits & 0x0000000f00000000L) >>> 32)];
        uuidChars[24]  = HEX_DIGITS[(int) ((leastSignificantBits & 0x00000000f0000000L) >>> 28)];
        uuidChars[25] = HEX_DIGITS[(int) ((leastSignificantBits & 0x000000000f000000L) >>> 24)];
        uuidChars[26] = HEX_DIGITS[(int) ((leastSignificantBits & 0x0000000000f00000L) >>> 20)];
        uuidChars[27] = HEX_DIGITS[(int) ((leastSignificantBits & 0x00000000000f0000L) >>> 16)];
        uuidChars[28] = HEX_DIGITS[(int) ((leastSignificantBits & 0x000000000000f000L) >>> 12)];
        uuidChars[29] = HEX_DIGITS[(int) ((leastSignificantBits & 0x0000000000000f00L) >>> 8)];
        uuidChars[30] = HEX_DIGITS[(int) ((leastSignificantBits & 0x00000000000000f0L) >>> 4)];
        uuidChars[31] = HEX_DIGITS[(int)  (leastSignificantBits & 0x000000000000000fL)];

        return new String(uuidChars);
    }

}
