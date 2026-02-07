package com.whoslucid.cobblemarket.util;

import com.cobblemon.mod.common.pokemon.Pokemon;
import com.whoslucid.cobblelib.util.PokemonUtils;
import com.whoslucid.cobblemarket.CobbleMarket;
import com.whoslucid.cobblemarket.listing.Listing;
import com.whoslucid.cobblemarket.listing.PokemonListing;
import net.minecraft.world.item.ItemStack;

import java.math.BigDecimal;
import java.util.List;

public class MarketUtils {

    /**
     * Replace placeholders in a string with listing data
     */
    public static String replaceListing(String text, Listing<?> listing) {
        if (text == null || listing == null) return text;

        text = text.replace("%listing_id%", listing.getId().toString());
        text = text.replace("%seller%", listing.getSellerName());
        text = text.replace("%price%", formatPrice(listing.getPrice()));
        text = text.replace("%currency%", listing.getCurrency().getCurrency());
        text = text.replace("%time_remaining%", TimeUtils.formatDuration(listing.getRemainingTime()));
        text = text.replace("%listing_type%", listing.getListingType().name());

        if (listing instanceof PokemonListing pokemonListing) {
            text = text.replace("%listing_name%", pokemonListing.getSpecies());
            text = text.replace("%level%", String.valueOf(pokemonListing.getLevel()));
            text = text.replace("%shiny_display%", pokemonListing.isShiny() ? "&a&lYES" : "&7No");
            text = text.replace("%ivs_perfect%", String.valueOf(pokemonListing.getPerfectIvCount()));

            Pokemon pokemon = pokemonListing.getPokemon();
            if (pokemon != null) {
                text = PokemonUtils.replace(text, pokemon);
            }
        } else {
            text = text.replace("%listing_name%", listing.getDisplayName());
        }

        return text;
    }

    /**
     * Replace placeholders in a list of strings
     */
    public static List<String> replaceListing(List<String> lore, Listing<?> listing) {
        return lore.stream()
                .map(line -> replaceListing(line, listing))
                .toList();
    }

    /**
     * Format a BigDecimal price for display
     */
    public static String formatPrice(BigDecimal price) {
        if (price == null) return "0";

        // Remove trailing zeros and format
        if (price.scale() <= 0 || price.stripTrailingZeros().scale() <= 0) {
            return String.format("%,d", price.longValue());
        }
        return String.format("%,.2f", price.doubleValue());
    }

    /**
     * Check if a Pokemon is blacklisted from being listed
     */
    public static boolean isPokemonBlacklisted(Pokemon pokemon) {
        if (pokemon == null) return true;

        // Check species blacklist
        String species = pokemon.getSpecies().getName().toLowerCase();
        if (CobbleMarket.config.getBannedPokemon().stream()
                .anyMatch(banned -> banned.equalsIgnoreCase(species))) {
            return true;
        }

        // Check the full blacklist from config
        return CobbleMarket.config.getPokemonBlacklist().isBlackListed(pokemon);
    }

    /**
     * Check if an item is blacklisted from being listed
     */
    public static boolean isItemBlacklisted(ItemStack item) {
        if (item == null || item.isEmpty()) return true;

        String itemId = item.getItem().builtInRegistryHolder().key().location().toString();
        return CobbleMarket.config.getBannedItems().stream()
                .anyMatch(banned -> banned.equalsIgnoreCase(itemId));
    }

    /**
     * Check if a player has reached their listing limit
     */
    public static boolean hasReachedListingLimit(java.util.UUID playerUuid) {
        if (CobbleMarket.listingManager == null) return false;

        int currentCount = CobbleMarket.listingManager.getPlayerListingCount(playerUuid);
        return currentCount >= CobbleMarket.config.getMaxListingsPerPlayer();
    }
}
