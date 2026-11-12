package viaprenetty.api;

public final class LegacyChunk12 {

    private LegacyChunk12() {
    }

    public static int size(final int primary, final int add, final boolean ground, final boolean sky) {
        return Integer.bitCount(primary & 0xFFFF) * (8192 + (sky ? 2048 : 0)) + Integer.bitCount(add & 0xFFFF) * 2048 + (ground ? 256 : 0);
    }

    public static byte[] convert(final byte[] raw, final int offset, final int primary, final int add, final boolean ground, final boolean sky, final BlockRemapper remapper) {
        final int sections = Integer.bitCount(primary & 0xFFFF);
        final int inSize = size(primary, add, ground, sky);
        if (offset + inSize > raw.length) {
            return null;
        }
        final byte[] out = new byte[size(primary, add, ground, true)];
        final int head = sections * 8192;
        System.arraycopy(raw, offset, out, 0, head);
        int in = offset + head;
        int pos = head;
        if (sky) {
            System.arraycopy(raw, in, out, pos, sections * 2048);
            in += sections * 2048;
        }
        pos += sections * 2048;
        System.arraycopy(raw, in, out, pos, out.length - pos);
        LegacyChunkRemapper.remapSections(out, 0, sections, remapper);
        return out;
    }

}
