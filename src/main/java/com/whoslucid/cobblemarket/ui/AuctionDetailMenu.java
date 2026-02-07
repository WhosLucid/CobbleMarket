package com.whoslucid.cobblemarket.ui;

import ca.landonjw.gooeylibs2.api.UIManager;
import ca.landonjw.gooeylibs2.api.button.GooeyButton;
import ca.landonjw.gooeylibs2.api.page.GooeyPage;
import ca.landonjw.gooeylibs2.api.template.types.ChestTemplate;
import com.whoslucid.cobblelib.api.EconomyApi;
import com.whoslucid.cobblelib.util.AdventureTranslator;
import com.whoslucid.cobblelib.util.PlayerUtils;
import com.whoslucid.cobblelib.util.TypeMessage;
import com.whoslucid.cobblemarket.CobbleMarket;
import com.whoslucid.cobblemarket.auction.Auction;
import com.whoslucid.cobblemarket.auction.Bid;
import com.whoslucid.cobblemarket.config.Lang;
import com.whoslucid.cobblemarket.util.MarketUtils;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemLore;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class AuctionDetailMenu {

    public static void open(ServerPlayer player, Auction auction) {
        CompletableFuture.runAsync(() -> {
            ChestTemplate template = buildTemplate(player, auction);
            String title = CobbleMarket.language.getTitleAuctionDetail()
                    .replace("%listing_name%", auction.getDisplayName());

            GooeyPage page = GooeyPage.builder()
                    .template(template)
                    .title(AdventureTranslator.toNative(title))
                    .build();

            player.getServer().execute(() -> UIManager.openUIForcefully(player, page));
        }, CobbleMarket.EXECUTOR);
    }

    private static ChestTemplate buildTemplate(ServerPlayer player, Auction auction) {
        ChestTemplate.Builder builder = ChestTemplate.builder(6);
        Lang.UIButtons buttons = CobbleMarket.language.getButtons();

        // Fill background
        GooeyButton filler = buttons.getFiller().getButton(action -> {});
        for (int i = 0; i < 54; i++) {
            builder.set(i, filler);
        }

        // Display Pokemon
        ItemStack displayItem = auction.getDisplayItem();
        List<String> itemLore = new ArrayList<>();
        itemLore.addAll(CobbleMarket.language.getPokemonLore());
        itemLore.add("");
        itemLore.addAll(CobbleMarket.language.getAuctionLore());

        List<String> replacedLore = MarketUtils.replaceListing(itemLore, auction);
        replacedLore = replacedLore.stream()
                .map(line -> line
                        .replace("%current_bid%", MarketUtils.formatPrice(auction.getCurrentBid()))
                        .replace("%bidder%", auction.getHighestBidderName() != null ?
                                auction.getHighestBidderName() : "No bids")
                        .replace("%min_next_bid%", MarketUtils.formatPrice(auction.getMinNextBid())))
                .toList();

        GooeyButton itemButton = GooeyButton.builder()
                .display(displayItem)
                .with(DataComponents.CUSTOM_NAME, AdventureTranslator.toNative("&6" + auction.getDisplayName()))
                .with(DataComponents.LORE, new ItemLore(AdventureTranslator.toNativeL(replacedLore)))
                .build();
        builder.set(13, itemButton);

        boolean isOwnAuction = auction.isSeller(player.getUUID());
        boolean isHighestBidder = auction.isHighestBidder(player.getUUID());

        if (isOwnAuction) {
            // Show auction status
            GooeyButton statusBtn = GooeyButton.builder()
                    .display(new ItemStack(Items.CLOCK))
                    .with(DataComponents.CUSTOM_NAME, AdventureTranslator.toNative("&eYour Auction"))
                    .with(DataComponents.LORE, new ItemLore(AdventureTranslator.toNativeL(Arrays.asList(
                            "&7Bids: &f" + auction.getBidCount(),
                            "&7Current Bid: &e" + MarketUtils.formatPrice(auction.getCurrentBid()),
                            "&7Time Left: &c" + auction.getFormattedRemainingTime()
                    ))))
                    .build();
            builder.set(22, statusBtn);
        } else {
            // Bid buttons
            BigDecimal minBid = auction.getMinNextBid();
            BigDecimal bid1 = minBid;
            BigDecimal bid2 = minBid.add(auction.getMinBidIncrement().multiply(BigDecimal.valueOf(5)));
            BigDecimal bid3 = minBid.add(auction.getMinBidIncrement().multiply(BigDecimal.valueOf(10)));

            builder.set(21, createBidButton(player, auction, bid1, "&a", "Min Bid"));
            builder.set(22, createBidButton(player, auction, bid2, "&e", "+5x"));
            builder.set(23, createBidButton(player, auction, bid3, "&6", "+10x"));

            if (isHighestBidder) {
                GooeyButton highestBtn = GooeyButton.builder()
                        .display(new ItemStack(Items.NETHER_STAR))
                        .with(DataComponents.CUSTOM_NAME, AdventureTranslator.toNative("&a&lYou are the highest bidder!"))
                        .with(DataComponents.LORE, new ItemLore(AdventureTranslator.toNativeL(Arrays.asList(
                                "&7Your bid: &e" + MarketUtils.formatPrice(auction.getCurrentBid())
                        ))))
                        .build();
                builder.set(31, highestBtn);
            }
        }

        // Bid history
        List<Bid> bidHistory = auction.getBidHistory();
        if (bidHistory != null && !bidHistory.isEmpty()) {
            int slot = 37;
            int shown = 0;
            for (int i = bidHistory.size() - 1; i >= 0 && shown < 5; i--, shown++) {
                Bid bid = bidHistory.get(i);
                GooeyButton bidBtn = GooeyButton.builder()
                        .display(new ItemStack(Items.PAPER))
                        .with(DataComponents.CUSTOM_NAME, AdventureTranslator.toNative("&f" + bid.getBidderName()))
                        .with(DataComponents.LORE, new ItemLore(AdventureTranslator.toNativeL(Arrays.asList(
                                "&7Bid: &e" + MarketUtils.formatPrice(bid.getAmount())
                        ))))
                        .build();
                builder.set(slot++, bidBtn);
            }
        }

        // Back button
        GooeyButton backBtn = buttons.getBack().getButton(action -> {
            AuctionsMenu.open(player);
        });
        builder.set(49, backBtn);

        return builder.build();
    }

    private static GooeyButton createBidButton(ServerPlayer player, Auction auction,
                                                BigDecimal bidAmount, String color, String label) {
        return GooeyButton.builder()
                .display(new ItemStack(Items.GOLD_INGOT))
                .with(DataComponents.CUSTOM_NAME, AdventureTranslator.toNative(color + label + ": " +
                        MarketUtils.formatPrice(bidAmount) + " " + auction.getCurrency().getCurrency()))
                .with(DataComponents.LORE, new ItemLore(AdventureTranslator.toNativeL(Arrays.asList(
                        "&7Click to place this bid"
                ))))
                .onClick(action -> {
                    placeBid(player, auction, bidAmount);
                })
                .build();
    }

    private static void placeBid(ServerPlayer player, Auction auction, BigDecimal bidAmount) {
        // Check if auction still exists
        var listing = CobbleMarket.listingManager.getListing(auction.getId());
        if (!(listing instanceof Auction currentAuction)) {
            PlayerUtils.sendMessage(player, CobbleMarket.language.getMessageListingNotFound()
                    .replace("%prefix%", CobbleMarket.language.getPrefix()), null, TypeMessage.CHAT);
            AuctionsMenu.open(player);
            return;
        }

        // Check if own auction
        if (currentAuction.isSeller(player.getUUID())) {
            PlayerUtils.sendMessage(player, CobbleMarket.language.getMessageCannotBidOwnAuction()
                    .replace("%prefix%", CobbleMarket.language.getPrefix()), null, TypeMessage.CHAT);
            return;
        }

        // Check if bid is high enough
        if (bidAmount.compareTo(currentAuction.getMinNextBid()) < 0) {
            PlayerUtils.sendMessage(player, CobbleMarket.language.getMessageBidTooLow()
                            .replace("%prefix%", CobbleMarket.language.getPrefix())
                            .replace("%min_bid%", MarketUtils.formatPrice(currentAuction.getMinNextBid()))
                            .replace("%currency%", currentAuction.getCurrency().getCurrency()),
                    null, TypeMessage.CHAT);
            return;
        }

        // Check if player can afford
        if (!EconomyApi.hasEnoughMoney(player.getUUID(), bidAmount, currentAuction.getCurrency(), false)) {
            PlayerUtils.sendMessage(player, CobbleMarket.language.getMessageNotEnoughMoney()
                            .replace("%prefix%", CobbleMarket.language.getPrefix())
                            .replace("%currency%", currentAuction.getCurrency().getCurrency()),
                    null, TypeMessage.CHAT);
            return;
        }

        // Get previous bidder for refund
        Bid previousBid = currentAuction.getPreviousBid();

        // Deduct money from new bidder
        EconomyApi.hasEnoughMoney(player.getUUID(), bidAmount, currentAuction.getCurrency(), true);

        // Refund previous bidder if exists
        if (previousBid != null && currentAuction.getHighestBidderUuid() != null) {
            EconomyApi.addMoney(currentAuction.getHighestBidderUuid(),
                    currentAuction.getCurrentBid(), currentAuction.getCurrency());

            // Notify previous bidder
            PlayerUtils.sendMessage(currentAuction.getHighestBidderUuid(),
                    CobbleMarket.language.getMessageBidOutbid()
                            .replace("%prefix%", CobbleMarket.language.getPrefix())
                            .replace("%listing_name%", currentAuction.getDisplayName())
                            .replace("%amount%", MarketUtils.formatPrice(bidAmount)),
                    null, TypeMessage.CHAT);
        }

        // Place bid
        currentAuction.placeBid(player.getUUID(), player.getName().getString(), bidAmount);

        // Save updated auction
        CobbleMarket.listingManager.saveListing(currentAuction);

        // Notify bidder
        PlayerUtils.sendMessage(player, CobbleMarket.language.getMessageBidPlaced()
                        .replace("%prefix%", CobbleMarket.language.getPrefix())
                        .replace("%amount%", MarketUtils.formatPrice(bidAmount))
                        .replace("%currency%", currentAuction.getCurrency().getCurrency())
                        .replace("%listing_name%", currentAuction.getDisplayName()),
                null, TypeMessage.CHAT);

        // Refresh menu
        open(player, currentAuction);
    }
}
