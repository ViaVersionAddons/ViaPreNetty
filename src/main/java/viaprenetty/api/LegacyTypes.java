package viaprenetty.api;

import com.viaversion.viaversion.api.minecraft.entitydata.EntityData;
import com.viaversion.viaversion.api.type.Type;
import com.viaversion.viaversion.api.type.Types;
import com.viaversion.viarewind.api.type.RewindTypes;
import io.netty.buffer.ByteBuf;

import java.util.ArrayList;
import java.util.List;

public final class LegacyTypes {

    public static final Type<String> STRING = new StringType();
    public static final Type<List<EntityData>> ENTITY_DATA_LIST = new EntityDataListType();

    private LegacyTypes() {
    }

    public static List<EntityData> nonEmpty(final List<EntityData> list) {
        if (list.isEmpty()) {
            list.add(new EntityData(1, LegacyEntityDataTypes.SHORT, (short) 300));
        }
        return list;
    }

    public static String limit(final String value, final int max) {
        if (value == null) {
            return "";
        }
        return value.length() > max ? value.substring(0, max) : value;
    }

    public static final class StringType extends Type<String> {

        public StringType() {
            super(String.class);
        }

        @Override
        public String read(final ByteBuf buffer) {
            final short length = buffer.readShort();
            final char[] chars = new char[Math.max(0, length)];
            for (int i = 0; i < chars.length; i++) {
                chars[i] = buffer.readChar();
            }
            return new String(chars);
        }

        @Override
        public void write(final ByteBuf buffer, final String value) {
            final String s = value == null ? "" : value;
            final int length = Math.min(s.length(), Short.MAX_VALUE);
            buffer.writeShort(length);
            for (int i = 0; i < length; i++) {
                buffer.writeChar(s.charAt(i));
            }
        }

    }

    public static final class EntityDataListType extends Type<List<EntityData>> {

        @SuppressWarnings("unchecked")
        public EntityDataListType() {
            super((Class<List<EntityData>>) (Class<?>) List.class);
        }

        @Override
        public List<EntityData> read(final ByteBuf buffer) {
            final List<EntityData> list = new ArrayList<>();
            while (true) {
                final int header = buffer.readUnsignedByte();
                if (header == 127) {
                    return list;
                }
                final LegacyEntityDataTypes type = LegacyEntityDataTypes.byId((header & 0xE0) >> 5);
                list.add(new EntityData(header & 0x1F, type, type.type().read(buffer)));
            }
        }

        @Override
        public void write(final ByteBuf buffer, final List<EntityData> list) {
            for (EntityData data : list) {
                final LegacyEntityDataTypes type = LegacyEntityDataTypes.byId(data.dataType().typeId());
                buffer.writeByte(((type.typeId() << 5) | (data.id() & 0x1F)) & 0xFF);
                writeValue(buffer, type, data.getValue());
            }
            buffer.writeByte(127);
        }

        @SuppressWarnings({"unchecked", "rawtypes"})
        private static void writeValue(final ByteBuf buffer, final LegacyEntityDataTypes type, final Object value) {
            switch (type) {
                case BYTE -> buffer.writeByte(((Number) value).byteValue());
                case SHORT -> buffer.writeShort(((Number) value).shortValue());
                case INT -> buffer.writeInt(((Number) value).intValue());
                case FLOAT -> buffer.writeFloat(((Number) value).floatValue());
                case STRING -> STRING.write(buffer, limit(String.valueOf(value), 64));
                default -> ((Type) type.type()).write(buffer, value);
            }
        }

    }

    public enum LegacyEntityDataTypes implements com.viaversion.viaversion.api.minecraft.entitydata.EntityDataType {

        BYTE(0, Types.BYTE),
        SHORT(1, Types.SHORT),
        INT(2, Types.INT),
        FLOAT(3, Types.FLOAT),
        STRING(4, LegacyTypes.STRING),
        ITEM(5, RewindTypes.COMPRESSED_NBT_ITEM),
        POSITION(6, Types.VECTOR);

        private final int id;
        private final Type<?> type;

        LegacyEntityDataTypes(final int id, final Type<?> type) {
            this.id = id;
            this.type = type;
        }

        public static LegacyEntityDataTypes byId(final int id) {
            return values()[id];
        }

        @Override
        public int typeId() {
            return this.id;
        }

        @Override
        public Type<?> type() {
            return this.type;
        }

    }

}
