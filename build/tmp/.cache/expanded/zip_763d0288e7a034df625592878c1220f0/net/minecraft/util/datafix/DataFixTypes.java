package net.minecraft.util.datafix;

import com.mojang.datafixers.DataFixer;
import com.mojang.datafixers.DSL.TypeReference;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.DynamicOps;
import java.util.Set;
import net.minecraft.SharedConstants;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.util.datafix.fixes.References;

public enum DataFixTypes {
    LEVEL(References.LEVEL),
    PLAYER(References.PLAYER),
    CHUNK(References.CHUNK),
    HOTBAR(References.HOTBAR),
    OPTIONS(References.OPTIONS),
    STRUCTURE(References.STRUCTURE),
    STATS(References.STATS),
    SAVED_DATA_COMMAND_STORAGE(References.SAVED_DATA_COMMAND_STORAGE),
    SAVED_DATA_FORCED_CHUNKS(References.SAVED_DATA_FORCED_CHUNKS),
    SAVED_DATA_MAP_DATA(References.SAVED_DATA_MAP_DATA),
    SAVED_DATA_MAP_INDEX(References.SAVED_DATA_MAP_INDEX),
    SAVED_DATA_RAIDS(References.SAVED_DATA_RAIDS),
    SAVED_DATA_RANDOM_SEQUENCES(References.SAVED_DATA_RANDOM_SEQUENCES),
    SAVED_DATA_SCOREBOARD(References.SAVED_DATA_SCOREBOARD),
    SAVED_DATA_STRUCTURE_FEATURE_INDICES(References.SAVED_DATA_STRUCTURE_FEATURE_INDICES),
    ADVANCEMENTS(References.ADVANCEMENTS),
    POI_CHUNK(References.POI_CHUNK),
    WORLD_GEN_SETTINGS(References.WORLD_GEN_SETTINGS),
    ENTITY_CHUNK(References.ENTITY_CHUNK);

    public static final Set<TypeReference> TYPES_FOR_LEVEL_LIST;
    private final TypeReference type;

    private DataFixTypes(TypeReference type) {
        this.type = type;
    }

    static int currentVersion() {
        return SharedConstants.getCurrentVersion().getDataVersion().getVersion();
    }

    public <A> Codec<A> wrapCodec(final Codec<A> codec, final DataFixer dataFixer, final int dataVersion) {
        return new Codec<A>() {
            @Override
            public <T> DataResult<T> encode(A p_301090_, DynamicOps<T> p_300954_, T p_301331_) {
                return codec.encode(p_301090_, p_300954_, p_301331_)
                    .flatMap(
                        p_300998_ -> p_300954_.mergeToMap(
                                (T)p_300998_, p_300954_.createString("DataVersion"), p_300954_.createInt(DataFixTypes.currentVersion())
                            )
                    );
            }

            @Override
            public <T> DataResult<Pair<A, T>> decode(DynamicOps<T> p_300987_, T p_301210_) {
                int i = p_300987_.get(p_301210_, "DataVersion").flatMap(p_300987_::getNumberValue).map(Number::intValue).result().orElse(dataVersion);
                Dynamic<T> dynamic = new Dynamic<>(p_300987_, p_300987_.remove(p_301210_, "DataVersion"));
                Dynamic<T> dynamic1 = DataFixTypes.this.updateToCurrentVersion(dataFixer, dynamic, i);
                return codec.decode(dynamic1);
            }
        };
    }

    public <T> Dynamic<T> update(DataFixer fixer, Dynamic<T> input, int version, int newVersion) {
        return fixer.update(this.type, input, version, newVersion);
    }

    public <T> Dynamic<T> updateToCurrentVersion(DataFixer fixer, Dynamic<T> input, int version) {
        return this.update(fixer, input, version, currentVersion());
    }

    public CompoundTag update(DataFixer fixer, CompoundTag tag, int version, int newVersion) {
        return (CompoundTag)this.update(fixer, new Dynamic<>(NbtOps.INSTANCE, tag), version, newVersion).getValue();
    }

    public CompoundTag updateToCurrentVersion(DataFixer fixer, CompoundTag tag, int version) {
        return this.update(fixer, tag, version, currentVersion());
    }

    static {
        TYPES_FOR_LEVEL_LIST = Set.of(LEVEL.type);
    }
}
