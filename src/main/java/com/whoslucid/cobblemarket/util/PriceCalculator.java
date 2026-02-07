package com.whoslucid.cobblemarket.util;

import com.cobblemon.mod.common.pokemon.Pokemon;
import com.whoslucid.cobblelib.util.PokemonUtils;
import com.whoslucid.cobblemarket.CobbleMarket;
import com.whoslucid.cobblemarket.config.Config;
import net.objecthunter.exp4j.Expression;
import net.objecthunter.exp4j.ExpressionBuilder;

import java.math.BigDecimal;

public class PriceCalculator {

    /**
     * Calculate the minimum price for a Pokemon based on its stats and rarity
     */
    public static BigDecimal calculateMinimumPrice(Pokemon pokemon) {
        if (pokemon == null) {
            return CobbleMarket.config.getMinimumPrice();
        }

        Config config = CobbleMarket.config;
        Config.PriceTiers tiers = config.getPriceTiers();

        // Start with base minimum
        BigDecimal minPrice = config.getMinimumPrice();

        // Calculate based on perfect IVs
        int perfectIvs = PokemonUtils.getTotalPerfectIvs(pokemon.getIvs());
        BigDecimal ivPrice = switch (perfectIvs) {
            case 1 -> tiers.getMinPrice1IV();
            case 2 -> tiers.getMinPrice2IV();
            case 3 -> tiers.getMinPrice3IV();
            case 4 -> tiers.getMinPrice4IV();
            case 5 -> tiers.getMinPrice5IV();
            case 6 -> tiers.getMinPrice6IV();
            default -> BigDecimal.ZERO;
        };
        minPrice = minPrice.max(ivPrice);

        // Add for hidden ability
        if (PokemonUtils.isAH(pokemon)) {
            minPrice = minPrice.max(tiers.getMinPriceHiddenAbility());
        }

        // Add for shiny
        if (pokemon.getShiny()) {
            minPrice = minPrice.max(tiers.getMinPriceShiny());
        }

        // Add for rarity
        String rarity = PokemonUtils.getRarityS(pokemon);
        switch (rarity.toLowerCase()) {
            case "legendary" -> minPrice = minPrice.max(tiers.getMinPriceLegendary());
            case "mythical" -> minPrice = minPrice.max(tiers.getMinPriceMythical());
            case "ultra_beast" -> minPrice = minPrice.max(tiers.getMinPriceUltraBeast());
        }

        return minPrice;
    }

    /**
     * Calculate suggested price using exp4j formula
     */
    public static BigDecimal calculateSuggestedPrice(Pokemon pokemon) {
        if (pokemon == null || !CobbleMarket.config.isUseFormulas()) {
            return calculateMinimumPrice(pokemon);
        }

        try {
            String formula = CobbleMarket.config.getPokemonFormula();
            Expression expression = new ExpressionBuilder(formula)
                    .variables("level", "ivs_total", "ivs_avg", "evs_total", "evs_avg",
                            "perfect_ivs", "shiny", "legendary", "hidden_ability", "mythical", "ultra_beast")
                    .build();

            // Set variables
            expression.setVariable("level", pokemon.getLevel());
            expression.setVariable("ivs_total", PokemonUtils.getIvsTotal(pokemon.getIvs()));
            expression.setVariable("ivs_avg", PokemonUtils.getIvsAverage(pokemon.getIvs()));
            expression.setVariable("evs_total", PokemonUtils.getEvsTotal(pokemon.getEvs()));
            expression.setVariable("evs_avg", PokemonUtils.getEvsAverage(pokemon.getEvs()));
            expression.setVariable("perfect_ivs", PokemonUtils.getTotalPerfectIvs(pokemon.getIvs()));
            expression.setVariable("shiny", pokemon.getShiny() ? 1 : 0);
            expression.setVariable("hidden_ability", PokemonUtils.isAH(pokemon) ? 1 : 0);

            String rarity = PokemonUtils.getRarityS(pokemon);
            expression.setVariable("legendary", rarity.equalsIgnoreCase("legendary") ? 1 : 0);
            expression.setVariable("mythical", rarity.equalsIgnoreCase("mythical") ? 1 : 0);
            expression.setVariable("ultra_beast", rarity.equalsIgnoreCase("ultra_beast") ? 1 : 0);

            double result = expression.evaluate();
            BigDecimal calculated = BigDecimal.valueOf(result);

            // Ensure it's at least the minimum
            BigDecimal minimum = calculateMinimumPrice(pokemon);
            return calculated.max(minimum);

        } catch (Exception e) {
            CobbleMarket.server.execute(() ->
                    com.whoslucid.cobblelib.CobbleLib.LOGGER.error("Error calculating price formula: " + e.getMessage())
            );
            return calculateMinimumPrice(pokemon);
        }
    }

    /**
     * Validate that a price is within allowed bounds
     */
    public static boolean isValidPrice(BigDecimal price, Pokemon pokemon) {
        if (price == null) return false;

        BigDecimal min = pokemon != null ? calculateMinimumPrice(pokemon) : CobbleMarket.config.getMinimumPrice();
        BigDecimal max = CobbleMarket.config.getMaximumPrice();

        return price.compareTo(min) >= 0 && price.compareTo(max) <= 0;
    }

    /**
     * Calculate tax on a sale
     */
    public static BigDecimal calculateTax(BigDecimal price) {
        if (price == null) return BigDecimal.ZERO;
        return price.multiply(BigDecimal.valueOf(CobbleMarket.config.getTaxRate()));
    }

    /**
     * Calculate seller's earnings after tax
     */
    public static BigDecimal calculateSellerEarnings(BigDecimal price) {
        if (price == null) return BigDecimal.ZERO;
        return price.subtract(calculateTax(price));
    }
}
