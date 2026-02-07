package com.whoslucid.cobblemarket.ui;

import ca.landonjw.gooeylibs2.api.UIManager;
import ca.landonjw.gooeylibs2.api.button.GooeyButton;
import ca.landonjw.gooeylibs2.api.page.GooeyPage;
import ca.landonjw.gooeylibs2.api.template.types.ChestTemplate;
import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.whoslucid.cobblelib.CobbleLib;
import com.whoslucid.cobblelib.Model.EconomyUse;
import com.whoslucid.cobblelib.ui.PartyPcMenu;
import com.whoslucid.cobblelib.ui.builds.PartyPcMenuBuilder;
import com.whoslucid.cobblelib.util.AdventureTranslator;
import com.whoslucid.cobblelib.util.PlayerUtils;
import com.whoslucid.cobblelib.util.TypeMessage;
import com.whoslucid.cobblemarket.CobbleMarket;
import com.whoslucid.cobblemarket.config.Lang;
import com.whoslucid.cobblemarket.auction.Auction;
import com.whoslucid.cobblemarket.listing.PokemonListing;
import com.whoslucid.cobblemarket.util.MarketUtils;
import com.whoslucid.cobblemarket.util.PriceCalculator;
import com.whoslucid.cobblemarket.util.TimeUtils;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.item.enchantment.ItemEnchantments;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;

public class CreateListingMenu {

    public static void open(ServerPlayer player) {
        // Check timeout
        if (CobbleMarket.timeoutManager.isTimedOut(player.getUUID())) {
            PlayerUtils.sendMessage(player, CobbleMarket.language.getMessagePlayerTimedOut()
                            .replace("%prefix%", CobbleMarket.language.getPrefix())
                            .replace("%remaining%", CobbleMarket.timeoutManager.getFormattedRemainingTimeout(player.getUUID())),
                    null, TypeMessage.CHAT);
            return;
        }

        // Check listing limit
        if (MarketUtils.hasReachedListingLimit(player.getUUID())) {
            PlayerUtils.sendMessage(player, CobbleMarket.language.getMessageMaxListingsReached()
                            .replace("%prefix%", CobbleMarket.language.getPrefix())
                            .replace("%max%", String.valueOf(CobbleMarket.config.getMaxListingsPerPlayer())),
                    null, TypeMessage.CHAT);
            return;
        }

        // Show selection menu for Pokemon or Item
        CompletableFuture.runAsync(() -> {
            try {
                ChestTemplate.Builder builder = ChestTemplate.builder(3);
                Lang.UIButtons buttons = CobbleMarket.language.getButtons();

                // Fill background
                GooeyButton filler = buttons.getFiller().getButton(action -> {});
                for (int i = 0; i < 27; i++) {
                    builder.set(i, filler);
                }

                // Pokemon button
                if (CobbleMarket.config.isEnablePokemonSales()) {
                    ItemStack pokeBallStack = buttons.getPokemonListings().getItemStack();
                    GooeyButton pokemonBtn = GooeyButton.builder()
                            .display(pokeBallStack.isEmpty() ? new ItemStack(Items.RED_DYE) : pokeBallStack)
                            .with(DataComponents.CUSTOM_NAME, AdventureTranslator.toNative("&bList a Pokemon"))
                            .with(DataComponents.LORE, new ItemLore(AdventureTranslator.toNativeL(Arrays.asList(
                                    "&7Select a Pokemon from your",
                                    "&7party or PC to list for sale",
                                    "",
                                    "&eClick to select Pokemon"
                            ))))
                            .onClick(action -> {
                                openPokemonSelection(player);
                            })
                            .build();
                    builder.set(11, pokemonBtn);
                }

                // Item button
                if (CobbleMarket.config.isEnableItemSales()) {
                    GooeyButton itemBtn = GooeyButton.builder()
                            .display(new ItemStack(Items.CHEST))
                            .with(DataComponents.CUSTOM_NAME, AdventureTranslator.toNative("&6List an Item"))
                            .with(DataComponents.LORE, new ItemLore(AdventureTranslator.toNativeL(Arrays.asList(
                                    "&7Select an item from your",
                                    "&7inventory to list for sale",
                                    "",
                                    "&eClick to select Item"
                            ))))
                            .onClick(action -> {
                                CreateItemListingMenu.open(player);
                            })
                            .build();
                    builder.set(15, itemBtn);
                }

                // Back button
                GooeyButton backBtn = buttons.getBack().getButton(action -> {
                    MarketMainMenu.open(player);
                });
                builder.set(22, backBtn);

                GooeyPage page = GooeyPage.builder()
                        .template(builder.build())
                        .title(AdventureTranslator.toNative("&0Create Listing"))
                        .build();

                player.getServer().execute(() -> UIManager.openUIForcefully(player, page));
            } catch (Exception e) {
                CobbleLib.LOGGER.error("Error opening CreateListingMenu: " + e.getMessage());
                e.printStackTrace();
            }
        }, CobbleMarket.EXECUTOR);
    }

    public static void openPokemonSelection(ServerPlayer player) {
        // Open party/PC menu
        PartyPcMenuBuilder builder = PartyPcMenu.builder()
                .setPlayer(player)
                .setPokemonAction(action -> {
                    Pokemon pokemon = action.getPokemon();
                    if (pokemon != null) {
                        openPriceSelection(player, pokemon, false, 24); // Default: fixed price, 24h duration
                    }
                })
                .setCloseAction(action -> {
                    open(player);
                })
                .setBlackList(CobbleMarket.config.getPokemonBlacklist())
                .setCustomFilter(pokemon -> !MarketUtils.isPokemonBlacklisted(pokemon));

        new PartyPcMenu().openParty(builder.build());
    }

    public static void openPriceSelection(ServerPlayer player, Pokemon pokemon, boolean isAuction, int auctionHours) {
        // Check if Pokemon is blacklisted
        if (MarketUtils.isPokemonBlacklisted(pokemon)) {
            PlayerUtils.sendMessage(player, CobbleMarket.language.getMessagePokemonBlacklisted()
                    .replace("%prefix%", CobbleMarket.language.getPrefix()), null, TypeMessage.CHAT);
            open(player);
            return;
        }

        // Check if auctions are enabled
        if (isAuction && !CobbleMarket.config.isEnableAuctions()) {
            PlayerUtils.sendMessage(player, CobbleMarket.language.getPrefix() + " &cAuctions are disabled.",
                    null, TypeMessage.CHAT);
            openPriceSelection(player, pokemon, false, auctionHours);
            return;
        }

        CompletableFuture.runAsync(() -> {
            BigDecimal suggestedPrice = PriceCalculator.calculateSuggestedPrice(pokemon);
            BigDecimal minPrice = PriceCalculator.calculateMinimumPrice(pokemon);

            ChestTemplate.Builder builder = ChestTemplate.builder(3);
            Lang.UIButtons buttons = CobbleMarket.language.getButtons();

            // Fill background
            GooeyButton filler = buttons.getFiller().getButton(action -> {});
            for (int i = 0; i < 27; i++) {
                builder.set(i, filler);
            }

            // Auction Toggle button (slot 4)
            ItemStack toggleItem = isAuction
                    ? new ItemStack(Items.LIME_STAINED_GLASS_PANE)
                    : new ItemStack(Items.RED_STAINED_GLASS_PANE);

            GooeyButton toggleBtn = GooeyButton.builder()
                    .display(toggleItem)
                    .with(DataComponents.CUSTOM_NAME, AdventureTranslator.toNative(
                            isAuction ? "&a✓ Auction Mode: ON" : "&c✗ Auction Mode: OFF"))
                    .with(DataComponents.LORE, new ItemLore(AdventureTranslator.toNativeL(Arrays.asList(
                            "",
                            isAuction ? "&7Price will be the &estarting bid" : "&7Price will be &efixed price",
                            "",
                            "&eClick to toggle"
                    ))))
                    .onClick(action -> {
                        openPriceSelection(player, pokemon, !isAuction, auctionHours);
                    })
                    .build();
            builder.set(4, toggleBtn);

            // Display Pokemon info
            String priceLabel = isAuction ? "Starting Bid" : "Price";
            GooeyButton pokemonBtn = GooeyButton.builder()
                    .display(com.cobblemon.mod.common.item.PokemonItem.from(pokemon, 1))
                    .with(DataComponents.CUSTOM_NAME, AdventureTranslator.toNative("&b" + pokemon.getSpecies().getName()))
                    .with(DataComponents.LORE, new ItemLore(AdventureTranslator.toNativeL(Arrays.asList(
                            "&7Suggested " + priceLabel + ": &e" + MarketUtils.formatPrice(suggestedPrice),
                            "&7Minimum " + priceLabel + ": &e" + MarketUtils.formatPrice(minPrice),
                            "",
                            isAuction ? "&6Auction Duration: &e" + auctionHours + " hours" : "",
                            "&eClick a " + priceLabel.toLowerCase() + " button to list"
                    ))))
                    .build();
            builder.set(13, pokemonBtn);

            // Price buttons (1x, 1.5x, 2x suggested)
            BigDecimal price1 = suggestedPrice.max(minPrice);
            BigDecimal price2 = suggestedPrice.multiply(BigDecimal.valueOf(1.5)).max(minPrice);
            BigDecimal price3 = suggestedPrice.multiply(BigDecimal.valueOf(2)).max(minPrice);

            builder.set(10, createPriceButton(player, pokemon, price1, "&a", isAuction, auctionHours));
            builder.set(11, createPriceButton(player, pokemon, price2, "&e", isAuction, auctionHours));
            builder.set(12, createPriceButton(player, pokemon, price3, "&6", isAuction, auctionHours));

            // Duration buttons (only shown in auction mode) - slots 14, 15, 16
            if (isAuction) {
                builder.set(14, createDurationButton(player, pokemon, 1, auctionHours));
                builder.set(15, createDurationButton(player, pokemon, 12, auctionHours));
                builder.set(16, createDurationButton(player, pokemon, 24, auctionHours));
            }

            // Back button
            GooeyButton backBtn = buttons.getBack().getButton(action -> {
                open(player);
            });
            builder.set(22, backBtn);

            String title = isAuction ? "&0Set Auction Starting Bid" : "&0Set Listing Price";
            GooeyPage page = GooeyPage.builder()
                    .template(builder.build())
                    .title(AdventureTranslator.toNative(title))
                    .build();

            player.getServer().execute(() -> UIManager.openUIForcefully(player, page));
        }, CobbleMarket.EXECUTOR);
    }

    private static GooeyButton createPriceButton(ServerPlayer player, Pokemon pokemon, BigDecimal price, String color,
                                                   boolean isAuction, int auctionHours) {
        String actionText = isAuction
                ? "&7Click to start auction"
                : "&7Click to list for this price";

        return GooeyButton.builder()
                .display(new ItemStack(Items.EMERALD))
                .with(DataComponents.CUSTOM_NAME, AdventureTranslator.toNative(color + MarketUtils.formatPrice(price) + " " +
                        CobbleMarket.config.getDefaultCurrency().getCurrency()))
                .with(DataComponents.LORE, new ItemLore(AdventureTranslator.toNativeL(Arrays.asList(
                        actionText
                ))))
                .onClick(action -> {
                    if (isAuction) {
                        createAuction(player, pokemon, price, auctionHours);
                    } else {
                        createListing(player, pokemon, price);
                    }
                })
                .build();
    }

    private static GooeyButton createDurationButton(ServerPlayer player, Pokemon pokemon, int hours, int selectedHours) {
        boolean isSelected = hours == selectedHours;
        ItemStack clockItem = new ItemStack(Items.CLOCK);

        // Add enchant glow if selected
        if (isSelected) {
            clockItem.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
        }

        String timeLabel = hours == 1 ? "1 Hour" : hours + " Hours";
        String color = isSelected ? "&a" : "&e";

        return GooeyButton.builder()
                .display(clockItem)
                .with(DataComponents.CUSTOM_NAME, AdventureTranslator.toNative(color + timeLabel))
                .with(DataComponents.LORE, new ItemLore(AdventureTranslator.toNativeL(Arrays.asList(
                        isSelected ? "&a✓ Selected" : "&7Click to select",
                        "",
                        "&7Auction will run for",
                        "&e" + timeLabel
                ))))
                .onClick(action -> {
                    if (!isSelected) {
                        openPriceSelection(player, pokemon, true, hours);
                    }
                })
                .build();
    }

    private static void createListing(ServerPlayer player, Pokemon pokemon, BigDecimal price) {
        // Final checks
        if (MarketUtils.isPokemonBlacklisted(pokemon)) {
            PlayerUtils.sendMessage(player, CobbleMarket.language.getMessagePokemonBlacklisted()
                    .replace("%prefix%", CobbleMarket.language.getPrefix()), null, TypeMessage.CHAT);
            return;
        }

        if (MarketUtils.hasReachedListingLimit(player.getUUID())) {
            PlayerUtils.sendMessage(player, CobbleMarket.language.getMessageMaxListingsReached()
                            .replace("%prefix%", CobbleMarket.language.getPrefix())
                            .replace("%max%", String.valueOf(CobbleMarket.config.getMaxListingsPerPlayer())),
                    null, TypeMessage.CHAT);
            return;
        }

        BigDecimal minPrice = PriceCalculator.calculateMinimumPrice(pokemon);
        if (price.compareTo(minPrice) < 0) {
            PlayerUtils.sendMessage(player, CobbleMarket.language.getMessagePriceTooLow()
                            .replace("%prefix%", CobbleMarket.language.getPrefix())
                            .replace("%min_price%", MarketUtils.formatPrice(minPrice))
                            .replace("%currency%", CobbleMarket.config.getDefaultCurrency().getCurrency()),
                    null, TypeMessage.CHAT);
            return;
        }

        // Remove Pokemon from party/PC
        var party = Cobblemon.INSTANCE.getStorage().getParty(player);
        var pc = Cobblemon.INSTANCE.getStorage().getPC(player);

        // Try to remove from party first
        party.remove(pokemon);

        // Also try to remove from PC (in case it's there)
        if (pc != null) {
            pc.remove(pokemon);
        }

        // Create listing
        EconomyUse currency = CobbleMarket.config.getDefaultCurrency();
        long duration = TimeUtils.hoursToMillis(CobbleMarket.config.getListingDurationHours());

        PokemonListing listing = new PokemonListing(
                player.getUUID(),
                player.getName().getString(),
                price,
                pokemon,
                currency,
                duration
        );

        CobbleMarket.listingManager.addListing(listing);

        // Send success message
        String msg = CobbleMarket.language.getMessageListingCreated()
                .replace("%prefix%", CobbleMarket.language.getPrefix())
                .replace("%price%", MarketUtils.formatPrice(price))
                .replace("%currency%", currency.getCurrency());
        PlayerUtils.sendMessage(player, msg, null, TypeMessage.CHAT);

        // Broadcast if enabled
        if (CobbleMarket.config.isBroadcastNewListings()) {
            String broadcast = CobbleMarket.language.getBroadcastNewListing()
                    .replace("%seller%", player.getName().getString())
                    .replace("%listing_name%", pokemon.getSpecies().getName())
                    .replace("%price%", MarketUtils.formatPrice(price))
                    .replace("%currency%", currency.getCurrency());
            PlayerUtils.sendMessage(player.getUUID(), broadcast, null, TypeMessage.BROADCAST);
        }

        MarketMainMenu.open(player);
    }

    private static void createAuction(ServerPlayer player, Pokemon pokemon, BigDecimal startingPrice, int durationHours) {
        // Final checks
        if (MarketUtils.isPokemonBlacklisted(pokemon)) {
            PlayerUtils.sendMessage(player, CobbleMarket.language.getMessagePokemonBlacklisted()
                    .replace("%prefix%", CobbleMarket.language.getPrefix()), null, TypeMessage.CHAT);
            return;
        }

        if (MarketUtils.hasReachedListingLimit(player.getUUID())) {
            PlayerUtils.sendMessage(player, CobbleMarket.language.getMessageMaxListingsReached()
                            .replace("%prefix%", CobbleMarket.language.getPrefix())
                            .replace("%max%", String.valueOf(CobbleMarket.config.getMaxListingsPerPlayer())),
                    null, TypeMessage.CHAT);
            return;
        }

        BigDecimal minPrice = PriceCalculator.calculateMinimumPrice(pokemon);
        if (startingPrice.compareTo(minPrice) < 0) {
            PlayerUtils.sendMessage(player, CobbleMarket.language.getMessagePriceTooLow()
                            .replace("%prefix%", CobbleMarket.language.getPrefix())
                            .replace("%min_price%", MarketUtils.formatPrice(minPrice))
                            .replace("%currency%", CobbleMarket.config.getDefaultCurrency().getCurrency()),
                    null, TypeMessage.CHAT);
            return;
        }

        // Remove Pokemon from party/PC
        var party = Cobblemon.INSTANCE.getStorage().getParty(player);
        var pc = Cobblemon.INSTANCE.getStorage().getPC(player);

        party.remove(pokemon);
        if (pc != null) {
            pc.remove(pokemon);
        }

        // Create auction
        EconomyUse currency = CobbleMarket.config.getDefaultCurrency();
        long duration = TimeUtils.hoursToMillis(durationHours);

        Auction auction = new Auction(
                player.getUUID(),
                player.getName().getString(),
                startingPrice,
                pokemon,
                currency,
                duration,
                CobbleMarket.config.getAuctionMinBidIncrement()
        );

        CobbleMarket.listingManager.addListing(auction);

        // Send success message
        String msg = CobbleMarket.language.getPrefix() + " &aAuction created! Starting bid: &e" +
                MarketUtils.formatPrice(startingPrice) + " " + currency.getCurrency() +
                " &7(Duration: " + durationHours + "h)";
        PlayerUtils.sendMessage(player, msg, null, TypeMessage.CHAT);

        // Broadcast if enabled
        if (CobbleMarket.config.isBroadcastNewListings()) {
            String broadcast = CobbleMarket.language.getBroadcastAuctionStart()
                    .replace("%seller%", player.getName().getString())
                    .replace("%listing_name%", pokemon.getSpecies().getName())
                    .replace("%price%", MarketUtils.formatPrice(startingPrice))
                    .replace("%currency%", currency.getCurrency());
            PlayerUtils.sendMessage(player.getUUID(), broadcast, null, TypeMessage.BROADCAST);
        }

        MarketMainMenu.open(player);
    }
}
