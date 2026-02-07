package net.minecraft.core.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import java.util.Map.Entry;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

public record TypedDataComponent<T>(DataComponentType<T> type, T value) {
    public static final StreamCodec<RegistryFriendlyByteBuf, TypedDataComponent<?>> STREAM_CODEC = new StreamCodec<RegistryFriendlyByteBuf, TypedDataComponent<?>>() {
        public TypedDataComponent<?> decode(RegistryFriendlyByteBuf p_331219_) {
            DataComponentType<?> datacomponenttype = DataComponentType.STREAM_CODEC.decode(p_331219_);
            return decodeTyped(p_331219_, datacomponenttype);
        }

        private static <T> TypedDataComponent<T> decodeTyped(RegistryFriendlyByteBuf p_331307_, DataComponentType<T> p_330560_) {
            return new TypedDataComponent<>(p_330560_, p_330560_.streamCodec().decode(p_331307_));
        }

        public void encode(RegistryFriendlyByteBuf p_330591_, TypedDataComponent<?> p_331491_) {
            encodeCap(p_330591_, p_331491_);
        }

        private static <T> void encodeCap(RegistryFriendlyByteBuf p_331348_, TypedDataComponent<T> p_331234_) {
            DataComponentType.STREAM_CODEC.encode(p_331348_, p_331234_.type());
            p_331234_.type().streamCodec().encode(p_331348_, p_331234_.value());
        }
    };

    static TypedDataComponent<?> fromEntryUnchecked(Entry<DataComponentType<?>, Object> entry) {
        return createUnchecked(entry.getKey(), entry.getValue());
    }

    public static <T> TypedDataComponent<T> createUnchecked(DataComponentType<T> type, Object value) {
        return new TypedDataComponent<>(type, (T)value);
    }

    public void applyTo(PatchedDataComponentMap map) {
        map.set(this.type, this.value);
    }

    public <D> DataResult<D> encodeValue(DynamicOps<D> ops) {
        Codec<T> codec = this.type.codec();
        return codec == null ? DataResult.error(() -> "Component of type " + this.type + " is not encodable") : codec.encodeStart(ops, this.value);
    }

    @Override
    public String toString() {
        return this.type + "=>" + this.value;
    }
}
