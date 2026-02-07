package com.whoslucid.cobblemarket.config;

import com.google.gson.Gson;
import com.whoslucid.cobblelib.CobbleLib;
import com.whoslucid.cobblelib.Model.ItemModel;
import com.whoslucid.cobblelib.ui.ConfirmMenu;
import com.whoslucid.cobblelib.util.Utils;
import com.whoslucid.cobblemarket.CobbleMarket;
import lombok.*;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Getter
@Setter
@EqualsAndHashCode
@ToString
public class Lang {
    private String prefix = "&7[<gradient:#4CAF50:#8BC34A>Market</gradient>&7]";

    // Titles
    private String titleMainMenu = "&0&lCobbleMarket";
    private String titlePokemonListings = "&0Pokemon Listings";
    private String titleItemListings = "&0Item Listings";
    private String titleAuctions = "&0Active Auctions";
    private String titleMyListings = "&0My Listings";
    private String titleExpiredListings = "&0Expired Listings";
    private String titleHistory = "&0Transaction History";
    private String titleListingDetail = "&0%listing_name%";
    private String titleAuctionDetail = "&0Auction: %listing_name%";
    private String titleSelectPokemon = "&0Select Pokemon to List";
    private String titleConfirmPurchase = "&0Confirm Purchase";

    // Messages
    private String messageListingCreated = "%prefix% &aListing created for &e%price% %currency%";
    private String messageListingPurchased = "%prefix% &aYou purchased &e%listing_name% &afor &e%price% %currency%";
    private String messageListingSold = "%prefix% &aYour &e%listing_name% &asold for &e%price% %currency%";
    private String messageListingExpired = "%prefix% &cYour listing for &e%listing_name% &chas expired";
    private String messageListingCancelled = "%prefix% &cListing cancelled";
    private String messageListingReclaimed = "%prefix% &aReclaimed &e%listing_name%";
    private String messageListingRelisted = "%prefix% &aRelisted &e%listing_name%";

    private String messageBidPlaced = "%prefix% &aBid of &e%amount% %currency% &aplaced on &e%listing_name%";
    private String messageBidOutbid = "%prefix% &cYou have been outbid on &e%listing_name%&c! Current bid: &e%amount%";
    private String messageAuctionWon = "%prefix% &aYou won the auction for &e%listing_name%&a!";
    private String messageAuctionEnded = "%prefix% &eAuction for &6%listing_name% &eended. Winner: &a%winner%";
    private String messageAuctionEndedNoBids = "%prefix% &cAuction for &e%listing_name% &cended with no bids";

    private String messageNotEnoughMoney = "%prefix% &cYou don't have enough %currency%";
    private String messageMaxListingsReached = "%prefix% &cYou've reached the maximum of %max% listings";
    private String messagePriceTooLow = "%prefix% &cMinimum price for this listing is &e%min_price% %currency%";
    private String messagePriceTooHigh = "%prefix% &cMaximum price is &e%max_price% %currency%";
    private String messageCannotBuyOwnListing = "%prefix% &cYou cannot buy your own listing";
    private String messageCannotBidOwnAuction = "%prefix% &cYou cannot bid on your own auction";
    private String messageListingNotFound = "%prefix% &cListing not found or already sold";
    private String messagePlayerTimedOut = "%prefix% &cYou are timed out from trading for &e%remaining%";
    private String messagePokemonBlacklisted = "%prefix% &cThis Pokemon cannot be listed";
    private String messageItemBlacklisted = "%prefix% &cThis item cannot be listed";
    private String messageBidTooLow = "%prefix% &cMinimum bid is &e%min_bid% %currency%";
    private String messageNoPartySpace = "%prefix% &cYou don't have enough party space";
    private String messageNoInventorySpace = "%prefix% &cYou don't have enough inventory space";

    private String messageReload = "%prefix% &aConfiguration reloaded";
    private String messageNoPermission = "%prefix% &cYou don't have permission to do that";
    private String messagePlayerNotFound = "%prefix% &cPlayer not found";
    private String messageTimeoutApplied = "%prefix% &aPlayer &e%player% &ahas been timed out for &e%duration%";
    private String messageListingRemoved = "%prefix% &aListing removed by admin";

    // Broadcast messages
    private String broadcastNewListing = "&7[&aMarket&7] &e%seller% &7listed &b%listing_name% &7for &a%price% %currency%";
    private String broadcastSale = "&7[&aMarket&7] &e%buyer% &7bought &b%listing_name% &7from &e%seller% &7for &a%price% %currency%";
    private String broadcastAuctionStart = "&7[&6Auction&7] &e%seller% &7started auction for &b%listing_name%&7! Starting: &a%price% %currency%";
    private String broadcastAuctionEnd = "&7[&6Auction&7] &b%listing_name% &7sold to &e%winner% &7for &a%price% %currency%";

    // UI Buttons
    private UIButtons buttons = new UIButtons();

    // Listing lore templates
    private List<String> listingLore = Arrays.asList(
            "&7Seller: &e%seller%",
            "&7Price: &a%price% %currency%",
            "&7Time Left: &e%time_remaining%",
            "",
            "&7Click to view details"
    );

    private List<String> pokemonLore = Arrays.asList(
            "&7Level: &e%level%",
            "&7Nature: &e%nature%",
            "&7Ability: &e%ability%",
            "&7Gender: &e%gender%",
            "&7Shiny: %shiny_display%",
            "&7IVs: &e%ivs_total%/186 &7(&e%ivs_perfect%&7 perfect)",
            "&7EVs: &e%evs_total%/510"
    );

    private List<String> auctionLore = Arrays.asList(
            "&6&lAUCTION",
            "&7Current Bid: &e%current_bid% %currency%",
            "&7Highest Bidder: &e%bidder%",
            "&7Time Left: &c%time_remaining%",
            "&7Min Next Bid: &e%min_next_bid%",
            "",
            "&eClick to place bid"
    );

    // Confirm menu
    private ConfirmMenu confirmMenu = new ConfirmMenu();

    public void init() {
        CompletableFuture<Boolean> futureRead = Utils.readFileAsync(CobbleMarket.PATH_LANG, CobbleMarket.config.getLang() + ".json", el -> {
            Gson gson = Utils.newGson();
            CobbleMarket.language = gson.fromJson(el, Lang.class);
            String data = gson.toJson(CobbleMarket.language);
            CompletableFuture<Boolean> futureWrite = Utils.writeFileAsync(CobbleMarket.PATH_LANG, CobbleMarket.config.getLang() + ".json", data);
            if (!futureWrite.join()) {
                CobbleLib.LOGGER.fatal("Could not write lang.json file for CobbleMarket.");
            }
        });
        if (!futureRead.join()) {
            CobbleLib.LOGGER.info("No lang.json file found for CobbleMarket. Attempting to generate one.");
            Gson gson = Utils.newGson();
            CobbleMarket.language = this;
            String data = gson.toJson(CobbleMarket.language);
            CompletableFuture<Boolean> futureWrite = Utils.writeFileAsync(CobbleMarket.PATH_LANG, CobbleMarket.config.getLang() + ".json", data);
            if (!futureWrite.join()) {
                CobbleLib.LOGGER.fatal("Could not write lang.json file for CobbleMarket.");
            }
        }
    }

    @Getter
    @Setter
    @EqualsAndHashCode
    @ToString
    public static class UIButtons {
        private ItemModel pokemonListings = new ItemModel(10, "cobblemon:poke_ball", "&bPokemon Listings",
                Arrays.asList("&7View all Pokemon for sale", "", "&eClick to browse"), 0);
        private ItemModel itemListings = new ItemModel(12, "minecraft:chest", "&6Item Listings",
                Arrays.asList("&7View all items for sale", "", "&eClick to browse"), 0);
        private ItemModel auctions = new ItemModel(14, "minecraft:gold_ingot", "&eActive Auctions",
                Arrays.asList("&7View ongoing auctions", "", "&eClick to browse"), 0);
        private ItemModel myListings = new ItemModel(28, "minecraft:book", "&aManage Listings",
                Arrays.asList("&7View your active listings", "", "&eClick to manage"), 0);
        private ItemModel expiredListings = new ItemModel(30, "minecraft:clock", "&cExpired Listings",
                Arrays.asList("&7Reclaim unsold items", "", "&eClick to view"), 0);
        private ItemModel history = new ItemModel(32, "minecraft:paper", "&dTransaction History",
                Arrays.asList("&7View past sales", "", "&eClick to view"), 0);
        private ItemModel createListing = new ItemModel(34, "minecraft:emerald", "&aCreate Listing",
                Arrays.asList("&7List a Pokemon or item", "", "&eClick to create"), 0);

        private ItemModel filterShiny = new ItemModel(48, "minecraft:nether_star", "&eShiny Only",
                Arrays.asList("&7Show only shiny Pokemon"), 0);
        private ItemModel filterLegendary = new ItemModel(50, "minecraft:dragon_egg", "&5Legendary Only",
                Arrays.asList("&7Show only legendaries"), 0);
        private ItemModel filterAll = new ItemModel(49, "minecraft:compass", "&fShow All",
                Arrays.asList("&7Remove filters"), 0);
        private ItemModel search = new ItemModel(51, "minecraft:spyglass", "&bSearch",
                Arrays.asList("&7Search by name"), 0);

        private ItemModel buy = new ItemModel(22, "minecraft:emerald", "&aBuy Now",
                Arrays.asList("&7Price: &e%price% %currency%", "", "&eClick to purchase"), 0);
        private ItemModel bid = new ItemModel(22, "minecraft:gold_ingot", "&6Place Bid",
                Arrays.asList("&7Minimum bid: &e%min_bid% %currency%", "", "&eClick to bid"), 0);
        private ItemModel cancel = new ItemModel(40, "minecraft:barrier", "&cCancel Listing",
                Arrays.asList("&7Remove this listing"), 0);
        private ItemModel relist = new ItemModel(22, "minecraft:clock", "&aRelist",
                Arrays.asList("&7Relist this expired item"), 0);
        private ItemModel reclaim = new ItemModel(24, "minecraft:hopper", "&aReclaim",
                Arrays.asList("&7Get your item back"), 0);

        private ItemModel previousPage = new ItemModel(45, "minecraft:arrow", "&ePrevious Page", Arrays.asList(), 0);
        private ItemModel nextPage = new ItemModel(53, "minecraft:arrow", "&eNext Page", Arrays.asList(), 0);
        private ItemModel back = new ItemModel(49, "minecraft:oak_door", "&cBack", Arrays.asList(), 0);
        private ItemModel close = new ItemModel(49, "minecraft:barrier", "&cClose", Arrays.asList(), 0);

        private ItemModel filler = new ItemModel("minecraft:gray_stained_glass_pane", " ", Arrays.asList());
    }
}
