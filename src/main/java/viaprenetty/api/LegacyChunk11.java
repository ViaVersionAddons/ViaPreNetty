package viaprenetty.api;

import java.util.Arrays;

public final class LegacyChunk11 {

    private LegacyChunk11() {
    }

    public static byte[] column(final byte[] raw, final int primary, final boolean skyDefault) {
        final byte[] out = new byte[32768 + 16384 * 3];
        if (skyDefault) {
            Arrays.fill(out, 65536, out.length, (byte) 0xFF);
        }
        final int sections = Integer.bitCount(primary & 0xFFFF);
        int k = 0;
        for (int s = 0; s < 16; s++) {
            if ((primary & (1 << s)) == 0) {
                continue;
            }
            if (s < 8) {
                for (int y = 0; y < 16; y++) {
                    for (int z = 0; z < 16; z++) {
                        for (int x = 0; x < 16; x++) {
                            final int i = (y << 8) | (z << 4) | x;
                            final int o = (x << 11) | (z << 7) | ((s << 4) + y);
                            copy(raw, sections, k, i, out, o, 32768);
                        }
                    }
                }
            }
            k++;
        }
        return out;
    }

    public static byte[] section(final byte[] raw, final int primary, final int section) {
        final int sections = Integer.bitCount(primary & 0xFFFF);
        int k = 0;
        for (int s = 0; s < section; s++) {
            if ((primary & (1 << s)) != 0) {
                k++;
            }
        }
        final byte[] out = new byte[4096 + 2048 * 3];
        for (int y = 0; y < 16; y++) {
            for (int z = 0; z < 16; z++) {
                for (int x = 0; x < 16; x++) {
                    final int i = (y << 8) | (z << 4) | x;
                    final int o = (x << 8) | (z << 4) | y;
                    copy(raw, sections, k, i, out, o, 4096);
                }
            }
        }
        return out;
    }

    private static void copy(final byte[] raw, final int sections, final int k, final int i, final byte[] out, final int o, final int volume) {
        out[o] = raw[k * 4096 + i];
        final int nibbleBase = sections * 4096;
        setNibble(out, volume, o, nibble(raw, nibbleBase + k * 2048, i));
        setNibble(out, volume + volume / 2, o, nibble(raw, nibbleBase + sections * 2048 + k * 2048, i));
        setNibble(out, volume * 2, o, nibble(raw, nibbleBase + sections * 4096 + k * 2048, i));
    }

    private static int nibble(final byte[] data, final int base, final int index) {
        final int value = data[base + (index >> 1)] & 0xFF;
        return (index & 1) == 0 ? value & 15 : value >> 4;
    }

    private static void setNibble(final byte[] data, final int base, final int index, final int value) {
        final int at = base + (index >> 1);
        if ((index & 1) == 0) {
            data[at] = (byte) ((data[at] & 0xF0) | value);
        } else {
            data[at] = (byte) ((data[at] & 0x0F) | (value << 4));
        }
    }

}
