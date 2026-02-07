package net.minecraft.util.datafix.schemas;

import com.mojang.datafixers.DSL.TypeReference;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.datafixers.types.templates.Const.PrimitiveType;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.codecs.PrimitiveCodec;
import net.minecraft.resources.ResourceLocation;

public class NamespacedSchema extends Schema {
    public static final PrimitiveCodec<String> NAMESPACED_STRING_CODEC = new PrimitiveCodec<String>() {
        @Override
        public <T> DataResult<String> read(DynamicOps<T> p_17321_, T p_17322_) {
            return p_17321_.getStringValue(p_17322_).map(NamespacedSchema::ensureNamespaced);
        }

        public <T> T write(DynamicOps<T> p_17318_, String p_17319_) {
            return p_17318_.createString(p_17319_);
        }

        @Override
        public String toString() {
            return "NamespacedString";
        }
    };
    private static final Type<String> NAMESPACED_STRING = new PrimitiveType<>(NAMESPACED_STRING_CODEC);

    public NamespacedSchema(int versionKey, Schema parent) {
        super(versionKey, parent);
    }

    public static String ensureNamespaced(String string) {
        ResourceLocation resourcelocation = ResourceLocation.tryParse(string);
        return resourcelocation != null ? resourcelocation.toString() : string;
    }

    public static Type<String> namespacedString() {
        return NAMESPACED_STRING;
    }

    @Override
    public Type<?> getChoiceType(TypeReference type, String choiceName) {
        return super.getChoiceType(type, ensureNamespaced(choiceName));
    }
}
