package com.whoslucid.cobblemarket.listing;

import com.cobblemon.mod.common.api.pokemon.PokemonSpecies;
import com.cobblemon.mod.common.item.PokemonItem;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import com.whoslucid.cobblelib.Model.EconomyUse;
import com.whoslucid.cobblelib.util.PokemonUtils;
import com.whoslucid.cobblemarket.CobbleMarket;
import com.whoslucid.cobblemarket.util.TimeUtils;
import lombok.*;
import net.minecraft.world.item.ItemStack;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class PokemonListing extends Listing<Pokemon> {

    // Serialized Pokemon data using Cobblemon's CODEC
    private JsonElement pokemonData;

    // Cached searchable attributes (populated on creation for faster filtering)
    private String species;
    private boolean isShiny;
    private boolean isLegendary;
    private boolean isMythical;
    private boolean isUltraBeast;
    private boolean hasHiddenAbility;
    private int perfectIvCount;
    private int level;
    private String nature;
    private String ability;

    /**
     * Create a new Pokemon listing
     */
    public PokemonListing(UUID sellerUuid, String sellerName, BigDecimal price,
                          Pokemon pokemon, EconomyUse currency, long durationMillis) {
        this.id = UUID.randomUUID();
        this.sellerUuid = sellerUuid;
        this.sellerName = sellerName;
        this.price = price;
        this.currency = currency;
        this.createdTime = System.currentTimeMillis();
        this.endTime = durationMillis > 0 ? createdTime + durationMillis : -1;
        this.listingType = ListingType.FIXED_PRICE;
        this.isPokemon = true;

        // Serialize Pokemon
        this.pokemonData = Pokemon.getCODEC().encodeStart(JsonOps.INSTANCE, pokemon)
                .resultOrPartial(error -> CobbleMarket.server.execute(() ->
                        com.whoslucid.cobblelib.CobbleLib.LOGGER.error("Error encoding Pokemon: " + error)))
                .orElse(null);

        // Cache searchable attributes
        cacheAttributes(pokemon);
    }

    /**
     * Constructor for deserialization (GSON)
     */
    public PokemonListing() {
        this.isPokemon = true;
    }

    /**
     * Cache searchable attributes from the Pokemon
     */
    private void cacheAttributes(Pokemon pokemon) {
        if (pokemon == null) return;

        this.species = pokemon.getSpecies().getName();
        this.isShiny = pokemon.getShiny();
        this.hasHiddenAbility = PokemonUtils.isAH(pokemon);
        this.perfectIvCount = PokemonUtils.getTotalPerfectIvs(pokemon.getIvs());
        this.level = pokemon.getLevel();
        this.nature = PokemonUtils.getNatureTranslate(pokemon.getNature());
        this.ability = pokemon.getAbility().getName();

        String rarity = PokemonUtils.getRarityS(pokemon);
        this.isLegendary = rarity.equalsIgnoreCase("legendary");
        this.isMythical = rarity.equalsIgnoreCase("mythical");
        this.isUltraBeast = rarity.equalsIgnoreCase("ultra_beast");
    }

    @Override
    public Pokemon getItem() {
        return getPokemon();
    }

    /**
     * Deserialize and return the Pokemon
     */
    public Pokemon getPokemon() {
        if (pokemonData == null) return null;

        try {
            return Pokemon.getCODEC().decode(JsonOps.INSTANCE, pokemonData)
                    .resultOrPartial(error -> CobbleMarket.server.execute(() ->
                            com.whoslucid.cobblelib.CobbleLib.LOGGER.error("Error decoding Pokemon: " + error)))
                    .map(pair -> pair.getFirst())
                    .orElse(null);
        } catch (Exception e) {
            com.whoslucid.cobblelib.CobbleLib.LOGGER.error("Failed to deserialize Pokemon: " + e.getMessage());
            return null;
        }
    }

    @Override
    public ItemStack getDisplayItem() {
        Pokemon pokemon = getPokemon();
        if (pokemon == null) {
            return ItemStack.EMPTY;
        }
        return PokemonItem.from(pokemon, 1);
    }

    @Override
    public String getDisplayName() {
        Pokemon pokemon = getPokemon();
        if (pokemon != null) {
            return pokemon.getSpecies().getName();
        }
        return species != null ? PokemonUtils.capitalize(species) : "Unknown Pokemon";
    }

    @Override
    public String getSearchableText() {
        StringBuilder sb = new StringBuilder();
        sb.append(species != null ? species.toLowerCase() : "");
        sb.append(" ");
        sb.append(nature != null ? nature.toLowerCase() : "");
        sb.append(" ");
        sb.append(ability != null ? ability.toLowerCase() : "");
        sb.append(" ");
        sb.append(sellerName != null ? sellerName.toLowerCase() : "");
        if (isShiny) sb.append(" shiny");
        if (isLegendary) sb.append(" legendary");
        if (isMythical) sb.append(" mythical");
        if (isUltraBeast) sb.append(" ultrabeast ultra_beast");
        if (hasHiddenAbility) sb.append(" hidden ability ha");
        return sb.toString();
    }

    @Override
    public boolean isValid() {
        return pokemonData != null && getPokemon() != null;
    }

    /**
     * Refresh cached attributes from current Pokemon data
     */
    public void refreshCache() {
        Pokemon pokemon = getPokemon();
        if (pokemon != null) {
            cacheAttributes(pokemon);
        }
    }
}
