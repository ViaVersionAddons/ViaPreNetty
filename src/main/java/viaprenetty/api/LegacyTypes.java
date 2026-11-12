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
    public static final Type<List<EntityData>> ENTITY_DATA_LIST = new EntityDataListType(true);
    public static final Type<List<EntityData>> ENTITY_DATA_LIST_NBTLESS = new EntityDataListType(false);
    public static final Type<com.viaversion.viaversion.api.minecraft.item.Item> NBTLESS_ITEM = new NbtLessItemType();

    public static final Type<com.viaversion.viaversion.api.minecraft.item.Item> ITEM_1_2 = new ConditionalNbtItemType();
    public static final Type<com.viaversion.viaversion.api.minecraft.item.Item[]> ITEM_ARRAY_1_2 = new ItemArrayType(ITEM_1_2);

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

        private final boolean nbt;

        @SuppressWarnings("unchecked")
        public EntityDataListType(final boolean nbt) {
            super((Class<List<EntityData>>) (Class<?>) List.class);
            this.nbt = nbt;
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
                list.add(new EntityData(header & 0x1F, type, type == LegacyEntityDataTypes.ITEM && !this.nbt ? NBTLESS_ITEM.read(buffer) : type.type().read(buffer)));
            }
        }

        @Override
        public void write(final ByteBuf buffer, final List<EntityData> list) {
            for (EntityData data : list) {
                final LegacyEntityDataTypes type = LegacyEntityDataTypes.byId(data.dataType().typeId());
                buffer.writeByte(((type.typeId() << 5) | (data.id() & 0x1F)) & 0xFF);
                if (type == LegacyEntityDataTypes.ITEM && !this.nbt) {
                    NBTLESS_ITEM.write(buffer, data.value());
                } else {
                    writeValue(buffer, type, data.getValue());
                }
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

    public static boolean hasNbt12(final int id) {
        return (id >= 256 && id <= 259) || id == 261 || (id >= 267 && id <= 279) || (id >= 283 && id <= 286) || (id >= 290 && id <= 294) || (id >= 298 && id <= 317) || id == 346 || id == 359;
    }

    public static final class ConditionalNbtItemType extends Type<com.viaversion.viaversion.api.minecraft.item.Item> {

        public ConditionalNbtItemType() {
            super(com.viaversion.viaversion.api.minecraft.item.Item.class);
        }

        @Override
        public com.viaversion.viaversion.api.minecraft.item.Item read(final ByteBuf buffer) {
            final short id = buffer.readShort();
            if (id < 0) {
                return null;
            }
            final byte amount = buffer.readByte();
            final short data = buffer.readShort();
            final com.viaversion.nbt.tag.CompoundTag tag = hasNbt12(id) ? RewindTypes.COMPRESSED_NBT.read(buffer) : null;
            return new com.viaversion.viaversion.api.minecraft.item.DataItem(id, amount, data, tag);
        }

        @Override
        public void write(final ByteBuf buffer, final com.viaversion.viaversion.api.minecraft.item.Item item) {
            if (item == null) {
                buffer.writeShort(-1);
                return;
            }
            buffer.writeShort(item.identifier());
            buffer.writeByte(item.amount());
            buffer.writeShort(item.data());
            if (hasNbt12(item.identifier())) {
                RewindTypes.COMPRESSED_NBT.write(buffer, item.tag());
            }
        }

    }

    public static final class ItemArrayType extends Type<com.viaversion.viaversion.api.minecraft.item.Item[]> {

        private final Type<com.viaversion.viaversion.api.minecraft.item.Item> element;

        public ItemArrayType(final Type<com.viaversion.viaversion.api.minecraft.item.Item> element) {
            super(com.viaversion.viaversion.api.minecraft.item.Item[].class);
            this.element = element;
        }

        @Override
        public com.viaversion.viaversion.api.minecraft.item.Item[] read(final ByteBuf buffer) {
            final com.viaversion.viaversion.api.minecraft.item.Item[] items = new com.viaversion.viaversion.api.minecraft.item.Item[buffer.readShort()];
            for (int i = 0; i < items.length; i++) {
                items[i] = this.element.read(buffer);
            }
            return items;
        }

        @Override
        public void write(final ByteBuf buffer, final com.viaversion.viaversion.api.minecraft.item.Item[] items) {
            buffer.writeShort(items.length);
            for (com.viaversion.viaversion.api.minecraft.item.Item item : items) {
                this.element.write(buffer, item);
            }
        }

    }

    public static final class NbtLessItemType extends Type<com.viaversion.viaversion.api.minecraft.item.Item> {

        public NbtLessItemType() {
            super(com.viaversion.viaversion.api.minecraft.item.Item.class);
        }

        @Override
        public com.viaversion.viaversion.api.minecraft.item.Item read(final ByteBuf buffer) {
            final short id = buffer.readShort();
            if (id < 0) {
                return null;
            }
            final byte amount = buffer.readByte();
            final short data = buffer.readShort();
            return new com.viaversion.viaversion.api.minecraft.item.DataItem(id, amount, data, null);
        }

        @Override
        public void write(final ByteBuf buffer, final com.viaversion.viaversion.api.minecraft.item.Item item) {
            if (item == null) {
                buffer.writeShort(-1);
                return;
            }
            buffer.writeShort(item.identifier());
            buffer.writeByte(item.amount());
            buffer.writeShort(item.data());
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
