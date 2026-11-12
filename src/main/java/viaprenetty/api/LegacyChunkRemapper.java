package viaprenetty.api;

import java.io.ByteArrayOutputStream;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

public final class LegacyChunkRemapper {

    private static final ThreadLocal<Inflater> INFLATER = ThreadLocal.withInitial(Inflater::new);
    private static final ThreadLocal<Deflater> DEFLATER = ThreadLocal.withInitial(() -> new Deflater(4));

    private LegacyChunkRemapper() {
    }

    public static byte[] remapSingle(final byte[] compressed, final int primaryBitmask, final BlockRemapper remapper) {
        if (primaryBitmask == 0) {
            return null;
        }
        final byte[] data = inflate(compressed);
        if (data == null) {
            return null;
        }
        final boolean changed = remapSections(data, 0, Integer.bitCount(primaryBitmask), remapper);
        return changed ? deflate(data) : null;
    }

    public static byte[] remapBulk(final byte[] compressed, final int[] primaryBitmasks, final int[] addBitmasks, final boolean skyLight, final BlockRemapper remapper) {
        final byte[] data = inflate(compressed);
        if (data == null) {
            return null;
        }
        boolean changed = false;
        int offset = 0;
        for (int i = 0; i < primaryBitmasks.length; i++) {
            final int sections = Integer.bitCount(primaryBitmasks[i] & 0xFFFF);
            final int adds = Integer.bitCount(addBitmasks[i] & 0xFFFF);
            final int size = sections * (4096 + 2048 + 2048 + (skyLight ? 2048 : 0)) + adds * 2048 + 256;
            if (offset + size > data.length) {
                break;
            }
            if (remapSections(data, offset, sections, remapper)) {
                changed = true;
            }
            offset += size;
        }
        return changed ? deflate(data) : null;
    }

    public static boolean remapSections(final byte[] data, final int offset, final int sections, final BlockRemapper remapper) {
        final int blockCount = sections * 4096;
        final int metaOffset = offset + blockCount;
        if (metaOffset + sections * 2048 > data.length) {
            return false;
        }
        boolean changed = false;
        for (int i = 0; i < blockCount; i++) {
            final int id = data[offset + i] & 0xFF;
            if (!remapper.affects(id)) {
                continue;
            }
            final int metaIndex = metaOffset + (i >> 1);
            final int metaByte = data[metaIndex] & 0xFF;
            final int meta = (i & 1) == 0 ? metaByte & 15 : metaByte >> 4;
            final int mapped = remapper.remap(id, meta);
            final int newId = mapped >> 4;
            final int newMeta = mapped & 15;
            if (newId == id && newMeta == meta) {
                continue;
            }
            data[offset + i] = (byte) newId;
            if ((i & 1) == 0) {
                data[metaIndex] = (byte) ((metaByte & 0xF0) | newMeta);
            } else {
                data[metaIndex] = (byte) ((metaByte & 0x0F) | (newMeta << 4));
            }
            changed = true;
        }
        return changed;
    }

    public static byte[] inflate(final byte[] compressed) {
        final Inflater inflater = INFLATER.get();
        inflater.reset();
        inflater.setInput(compressed);
        final ByteArrayOutputStream out = new ByteArrayOutputStream(Math.max(1024, compressed.length * 4));
        final byte[] buffer = new byte[16384];
        try {
            while (!inflater.finished()) {
                final int read = inflater.inflate(buffer);
                if (read == 0) {
                    if (inflater.needsInput() || inflater.needsDictionary()) {
                        break;
                    }
                }
                out.write(buffer, 0, read);
            }
        } catch (final DataFormatException e) {
            return null;
        }
        return out.toByteArray();
    }

    public static byte[] deflate(final byte[] data) {
        final Deflater deflater = DEFLATER.get();
        deflater.reset();
        deflater.setInput(data);
        deflater.finish();
        final ByteArrayOutputStream out = new ByteArrayOutputStream(data.length / 2 + 64);
        final byte[] buffer = new byte[16384];
        while (!deflater.finished()) {
            final int written = deflater.deflate(buffer);
            out.write(buffer, 0, written);
        }
        return out.toByteArray();
    }

    public static byte[] addSkyLight(final byte[] raw, final int[] primaryBitmasks, final int[] addBitmasks) {
        int total = 0;
        for (int i = 0; i < primaryBitmasks.length; i++) {
            total += Integer.bitCount(primaryBitmasks[i]) * 10240 + Integer.bitCount(addBitmasks[i]) * 2048 + 256;
        }
        final byte[] out = new byte[total];
        int in = 0;
        int pos = 0;
        for (int i = 0; i < primaryBitmasks.length; i++) {
            final int sections = Integer.bitCount(primaryBitmasks[i]);
            final int head = sections * 8192;
            final int tail = Integer.bitCount(addBitmasks[i]) * 2048 + 256;
            if (in + head + tail > raw.length) {
                return null;
            }
            System.arraycopy(raw, in, out, pos, head);
            pos += head + sections * 2048;
            in += head;
            System.arraycopy(raw, in, out, pos, tail);
            pos += tail;
            in += tail;
        }
        return out;
    }

}
