package net.minecraft.client.telemetry;

import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.MapLike;
import com.mojang.serialization.RecordBuilder;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class TelemetryPropertyMap {
    final Map<TelemetryProperty<?>, Object> entries;

    TelemetryPropertyMap(Map<TelemetryProperty<?>, Object> entries) {
        this.entries = entries;
    }

    public static TelemetryPropertyMap.Builder builder() {
        return new TelemetryPropertyMap.Builder();
    }

    public static MapCodec<TelemetryPropertyMap> createCodec(final List<TelemetryProperty<?>> properties) {
        return new MapCodec<TelemetryPropertyMap>() {
            public <T> RecordBuilder<T> encode(TelemetryPropertyMap p_261525_, DynamicOps<T> p_262068_, RecordBuilder<T> p_261850_) {
                RecordBuilder<T> recordbuilder = p_261850_;

                for (TelemetryProperty<?> telemetryproperty : properties) {
                    recordbuilder = this.encodeProperty(p_261525_, recordbuilder, telemetryproperty);
                }

                return recordbuilder;
            }

            private <T, V> RecordBuilder<T> encodeProperty(TelemetryPropertyMap p_262128_, RecordBuilder<T> p_261947_, TelemetryProperty<V> p_261911_) {
                V v = p_262128_.get(p_261911_);
                return v != null ? p_261947_.add(p_261911_.id(), v, p_261911_.codec()) : p_261947_;
            }

            @Override
            public <T> DataResult<TelemetryPropertyMap> decode(DynamicOps<T> p_261767_, MapLike<T> p_262176_) {
                DataResult<TelemetryPropertyMap.Builder> dataresult = DataResult.success(new TelemetryPropertyMap.Builder());

                for (TelemetryProperty<?> telemetryproperty : properties) {
                    dataresult = this.decodeProperty(dataresult, p_261767_, p_262176_, telemetryproperty);
                }

                return dataresult.map(TelemetryPropertyMap.Builder::build);
            }

            private <T, V> DataResult<TelemetryPropertyMap.Builder> decodeProperty(
                DataResult<TelemetryPropertyMap.Builder> p_261892_, DynamicOps<T> p_261859_, MapLike<T> p_261668_, TelemetryProperty<V> p_261627_
            ) {
                T t = p_261668_.get(p_261627_.id());
                if (t != null) {
                    DataResult<V> dataresult = p_261627_.codec().parse(p_261859_, t);
                    return p_261892_.apply2stable((p_262028_, p_261796_) -> p_262028_.put(p_261627_, (V)p_261796_), dataresult);
                } else {
                    return p_261892_;
                }
            }

            @Override
            public <T> Stream<T> keys(DynamicOps<T> p_261746_) {
                return properties.stream().map(TelemetryProperty::id).map(p_261746_::createString);
            }
        };
    }

    @Nullable
    public <T> T get(TelemetryProperty<T> key) {
        return (T)this.entries.get(key);
    }

    @Override
    public String toString() {
        return this.entries.toString();
    }

    public Set<TelemetryProperty<?>> propertySet() {
        return this.entries.keySet();
    }

    @OnlyIn(Dist.CLIENT)
    public static class Builder {
        private final Map<TelemetryProperty<?>, Object> entries = new Reference2ObjectOpenHashMap<>();

        Builder() {
        }

        public <T> TelemetryPropertyMap.Builder put(TelemetryProperty<T> key, T value) {
            this.entries.put(key, value);
            return this;
        }

        public <T> TelemetryPropertyMap.Builder putIfNotNull(TelemetryProperty<T> key, @Nullable T value) {
            if (value != null) {
                this.entries.put(key, value);
            }

            return this;
        }

        public TelemetryPropertyMap.Builder putAll(TelemetryPropertyMap propertyMap) {
            this.entries.putAll(propertyMap.entries);
            return this;
        }

        public TelemetryPropertyMap build() {
            return new TelemetryPropertyMap(this.entries);
        }
    }
}
