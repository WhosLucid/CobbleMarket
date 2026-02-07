package com.whoslucid.cobblemarket.config;

import com.google.gson.Gson;
import com.whoslucid.cobblelib.CobbleLib;
import com.whoslucid.cobblelib.Model.EconomyUse;
import com.whoslucid.cobblelib.Model.PokemonBlackList;
import com.whoslucid.cobblelib.util.Utils;
import com.whoslucid.cobblemarket.CobbleMarket;
import lombok.*;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Getter
@Setter
@EqualsAndHashCode
@ToString
public class Config {
    private boolean debug = false;
    private String lang = "en";
    private String[] commands = new String[]{"market", "gts", "cobblemarket"};

    // Listing settings
    private boolean enablePokemonSales = true;
    private boolean enableItemSales = true;
    private boolean enableAuctions = true;
    private int maxListingsPerPlayer = 8;
    private int listingDurationHours = 72;
    private int auctionMinDurationMinutes = 30;
    private int auctionMaxDurationHours = 168;

    // Economy settings
    private EconomyUse defaultCurrency = new EconomyUse("Cobbletokens", "");
    private boolean allowMultipleCurrencies = true;
    private List<String> supportedCurrencies = Arrays.asList("Cobbletokens", "XP");

    // Pricing settings
    private boolean useFormulas = true;
    private double taxRate = 0.10;
    private BigDecimal minimumPrice = BigDecimal.valueOf(100);
    private BigDecimal maximumPrice = BigDecimal.valueOf(10000000);
    private BigDecimal auctionMinBidIncrement = BigDecimal.valueOf(100);
    private String pokemonFormula = "100 + (level * 10) + (perfect_ivs * 500) + (shiny * 5000) + (legendary * 10000) + (hidden_ability * 3000)";

    // Price tiers
    private PriceTiers priceTiers = new PriceTiers();

    // Blacklists
    private PokemonBlackList pokemonBlacklist = new PokemonBlackList();
    private List<String> bannedItems = Arrays.asList("minecraft:bedrock", "cobblemon:master_ball");
    private List<String> bannedPokemon = Arrays.asList();

    // Moderation
    private boolean enableTimeouts = true;
    private int defaultTimeoutMinutes = 60;
    private boolean broadcastNewListings = true;
    private boolean broadcastSales = true;

    // Discord
    private DiscordConfig discord = new DiscordConfig();

    public void init() {
        CompletableFuture<Boolean> futureRead = Utils.readFileAsync(CobbleMarket.PATH, "config.json", el -> {
            Gson gson = Utils.newGson();
            CobbleMarket.config = gson.fromJson(el, Config.class);
            String data = gson.toJson(CobbleMarket.config);
            CompletableFuture<Boolean> futureWrite = Utils.writeFileAsync(CobbleMarket.PATH, "config.json", data);
            if (!futureWrite.join()) {
                CobbleLib.LOGGER.fatal("Could not write config.json file for CobbleMarket.");
            }
        });
        if (!futureRead.join()) {
            CobbleLib.LOGGER.info("No config.json file found for CobbleMarket. Attempting to generate one.");
            Gson gson = Utils.newGson();
            CobbleMarket.config = this;
            String data = gson.toJson(CobbleMarket.config);
            CompletableFuture<Boolean> futureWrite = Utils.writeFileAsync(CobbleMarket.PATH, "config.json", data);
            if (!futureWrite.join()) {
                CobbleLib.LOGGER.fatal("Could not write config.json file for CobbleMarket.");
            }
        }
    }

    @Getter
    @Setter
    @EqualsAndHashCode
    @ToString
    public static class PriceTiers {
        private BigDecimal minPrice1IV = BigDecimal.valueOf(1000);
        private BigDecimal minPrice2IV = BigDecimal.valueOf(2000);
        private BigDecimal minPrice3IV = BigDecimal.valueOf(4000);
        private BigDecimal minPrice4IV = BigDecimal.valueOf(8000);
        private BigDecimal minPrice5IV = BigDecimal.valueOf(15000);
        private BigDecimal minPrice6IV = BigDecimal.valueOf(30000);
        private BigDecimal minPriceHiddenAbility = BigDecimal.valueOf(5000);
        private BigDecimal minPriceShiny = BigDecimal.valueOf(10000);
        private BigDecimal minPriceLegendary = BigDecimal.valueOf(25000);
        private BigDecimal minPriceMythical = BigDecimal.valueOf(50000);
        private BigDecimal minPriceUltraBeast = BigDecimal.valueOf(20000);
    }

    @Getter
    @Setter
    @EqualsAndHashCode
    @ToString
    public static class DiscordConfig {
        private boolean enabled = false;
        private String webhookUrl = "";
        private boolean notifyNewListings = true;
        private boolean notifySales = true;
        private boolean notifyAuctionEnd = true;
    }
}
