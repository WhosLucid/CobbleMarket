package com.whoslucid.cobblemarket.ui;

import ca.landonjw.gooeylibs2.api.UIManager;
import ca.landonjw.gooeylibs2.api.button.GooeyButton;
import ca.landonjw.gooeylibs2.api.page.GooeyPage;
import ca.landonjw.gooeylibs2.api.template.types.ChestTemplate;
import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.whoslucid.cobblelib.api.EconomyApi;
import com.whoslucid.cobblelib.util.AdventureTranslator;
import com.whoslucid.cobblelib.util.PlayerUtils;
import com.whoslucid.cobblelib.util.TypeMessage;
import com.whoslucid.cobblemarket.CobbleMarket;
import com.whoslucid.cobblemarket.config.Lang;
import com.whoslucid.cobblemarket.history.TransactionRecord;
import com.whoslucid.cobblemarket.listing.ItemListing;
import com.whoslucid.cobblemarket.listing.Listing;
import com.whoslucid.cobblemarket.listing.PokemonListing;
import com.whoslucid.cobblemarket.util.MarketUtils;
import com.whoslucid.cobblemarket.util.PriceCalculator;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemLore;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class ListingDetailMenu {

    public static void open(ServerPlayer player, Listing<?> listing) {
        CompletableFuture.runAsync(() -> {
            ChestTemplate template = buildTemplate(player, listing);
            String title = CobbleMarket.language.getTitleListingDetail()
                    .replace("%listing_name%", listing.getDisplayName());

            GooeyPage page = GooeyPage.builder()
                    .template(template)
                    .title(AdventureTranslator.toNative(title))
                    .build();

            player.getServer().execute(() -> UIManager.openUIForcefully(player, page));
        }, CobbleMarket.EXECUTOR);
    }

    private static ChestTemplate buildTemplate(ServerPlayer player, Listing<?> listing) {
        ChestTemplate.Builder builder = ChestTemplate.builder(6);
        Lang.UIButtons buttons = CobbleMarket.language.getButtons();

        // Fill background
        GooeyButton filler = buttons.getFiller().getButton(action -> {});
        for (int i = 0; i < 54; i++) {
            builder.set(i, filler);
        }

        // Display item in center
        ItemStack displayItem = listing.getDisplayItem();
        List<String> itemLore = new ArrayList<>();
        if (listing instanceof PokemonListing) {
            itemLore.addAll(CobbleMarket.language.getPokemonLore());
        }
        itemLore.add("");
        itemLore.addAll(CobbleMarket.language.getListingLore());

        GooeyButton itemButton = GooeyButton.builder()
                .display(displayItem)
                .with(DataComponents.CUSTOM_NAME, AdventureTranslator.toNative("&b" + listing.getDisplayName()))
                .with(DataComponents.LORE, new ItemLore(AdventureTranslator.toNativeL(MarketUtils.replaceListing(itemLore, listing))))
                .build();
        builder.set(13, itemButton);

        // Buy button (or cancel if own listing)
        boolean isOwnListing = listing.isSeller(player.getUUID());

        if (isOwnListing) {
            // Cancel button
            GooeyButton cancelBtn = GooeyButton.builder()
                    .display(buttons.getCancel().getItemStack())
                    .with(DataComponents.CUSTOM_NAME, AdventureTranslator.toNative(buttons.getCancel().getDisplayname()))
                    .with(DataComponents.LORE, new ItemLore(AdventureTranslator.toNativeL(buttons.getCancel().getLore())))
                    .onClick(action -> {
                        cancelListing(player, listing);
                    })
                    .build();
            builder.set(22, cancelBtn);
        } else {
            // Buy button
            List<String> buyLore = new ArrayList<>(buttons.getBuy().getLore());
            buyLore = MarketUtils.replaceListing(buyLore, listing);

            GooeyButton buyBtn = GooeyButton.builder()
                    .display(buttons.getBuy().getItemStack())
                    .with(DataComponents.CUSTOM_NAME, AdventureTranslator.toNative(buttons.getBuy().getDisplayname()))
                    .with(DataComponents.LORE, new ItemLore(AdventureTranslator.toNativeL(buyLore)))
                    .onClick(action -> {
                        confirmPurchase(player, listing);
                    })
                    .build();
            builder.set(22, buyBtn);
        }

        // Back button
        GooeyButton backBtn = buttons.getBack().getButton(action -> {
            if (listing instanceof PokemonListing) {
                PokemonListingsMenu.open(player);
            } else {
                ItemListingsMenu.open(player);
            }
        });
        builder.set(49, backBtn);

        return builder.build();
    }

    private static void confirmPurchase(ServerPlayer player, Listing<?> listing) {
        // Check if listing still exists
        Listing<?> current = CobbleMarket.listingManager.getListing(listing.getId());
        if (current == null) {
            PlayerUtils.sendMessage(player, CobbleMarket.language.getMessageListingNotFound()
                    .replace("%prefix%", CobbleMarket.language.getPrefix()), null, TypeMessage.CHAT);
            MarketMainMenu.open(player);
            return;
        }

        // Check if player can afford
        BigDecimal price = listing.getPrice();
        if (!EconomyApi.hasEnoughMoney(player.getUUID(), price, listing.getCurrency(), false)) {
            PlayerUtils.sendMessage(player, CobbleMarket.language.getMessageNotEnoughMoney()
                            .replace("%prefix%", CobbleMarket.language.getPrefix())
                            .replace("%currency%", listing.getCurrency().getCurrency()),
                    null, TypeMessage.CHAT);
            return;
        }

        // Open confirm menu
        CobbleMarket.language.getConfirmMenu().open(
                player,
                listing.getDisplayItem(),
                confirmAction -> {
                    executePurchase(player, listing);
                },
                cancelAction -> {
                    open(player, listing);
                }
        );
    }

    private static void executePurchase(ServerPlayer player, Listing<?> listing) {
        // Double-check listing still exists
        Listing<?> current = CobbleMarket.listingManager.getListing(listing.getId());
        if (current == null) {
            PlayerUtils.sendMessage(player, CobbleMarket.language.getMessageListingNotFound()
                    .replace("%prefix%", CobbleMarket.language.getPrefix()), null, TypeMessage.CHAT);
            MarketMainMenu.open(player);
            return;
        }

        BigDecimal price = listing.getPrice();

        // Deduct money from buyer
        if (!EconomyApi.hasEnoughMoney(player.getUUID(), price, listing.getCurrency(), true)) {
            PlayerUtils.sendMessage(player, CobbleMarket.language.getMessageNotEnoughMoney()
                            .replace("%prefix%", CobbleMarket.language.getPrefix())
                            .replace("%currency%", listing.getCurrency().getCurrency()),
                    null, TypeMessage.CHAT);
            return;
        }

        // Transfer item to buyer
        if (listing instanceof PokemonListing pokemonListing) {
            Pokemon pokemon = pokemonListing.getPokemon();
            if (pokemon == null) {
                // Refund buyer
                EconomyApi.addMoney(player.getUUID(), price, listing.getCurrency());
                PlayerUtils.sendMessage(player, CobbleMarket.language.getMessageListingNotFound()
                        .replace("%prefix%", CobbleMarket.language.getPrefix()), null, TypeMessage.CHAT);
                return;
            }

            // Add to party or PC
            var party = Cobblemon.INSTANCE.getStorage().getParty(player);
            if (!party.add(pokemon)) {
                // Party full, try PC
                var pc = Cobblemon.INSTANCE.getStorage().getPC(player);
                if (pc != null) {
                    pc.add(pokemon);
                } else {
                    // Refund
                    EconomyApi.addMoney(player.getUUID(), price, listing.getCurrency());
                    PlayerUtils.sendMessage(player, CobbleMarket.language.getMessageNoPartySpace()
                            .replace("%prefix%", CobbleMarket.language.getPrefix()), null, TypeMessage.CHAT);
                    return;
                }
            }
        } else if (listing instanceof ItemListing itemListing) {
            ItemStack item = itemListing.getItemStack();
            if (item.isEmpty()) {
                EconomyApi.addMoney(player.getUUID(), price, listing.getCurrency());
                PlayerUtils.sendMessage(player, CobbleMarket.language.getMessageListingNotFound()
                        .replace("%prefix%", CobbleMarket.language.getPrefix()), null, TypeMessage.CHAT);
                return;
            }

            // Add to inventory
            if (!player.getInventory().add(item)) {
                // No space
                EconomyApi.addMoney(player.getUUID(), price, listing.getCurrency());
                PlayerUtils.sendMessage(player, CobbleMarket.language.getMessageNoInventorySpace()
                        .replace("%prefix%", CobbleMarket.language.getPrefix()), null, TypeMessage.CHAT);
                return;
            }
        }

        // Pay seller (minus tax)
        BigDecimal sellerEarnings = PriceCalculator.calculateSellerEarnings(price);
        EconomyApi.addMoney(listing.getSellerUuid(), sellerEarnings, listing.getCurrency());

        // Remove listing
        CobbleMarket.listingManager.removeListing(listing.getId());

        // Record transactions
        BigDecimal tax = PriceCalculator.calculateTax(price);
        CobbleMarket.historyManager.addTransaction(player.getUUID(),
                TransactionRecord.purchase(listing.getDisplayName(), price, listing.getCurrency().getCurrency(),
                        listing.getSellerName(), listing.getSellerUuid(), listing.isPokemon(), listing.getListingType()));

        CobbleMarket.historyManager.addTransaction(listing.getSellerUuid(),
                TransactionRecord.sale(listing.getDisplayName(), price, listing.getCurrency().getCurrency(),
                        tax, player.getName().getString(), player.getUUID(), listing.isPokemon(), listing.getListingType()));

        // Send messages
        String buyerMsg = CobbleMarket.language.getMessageListingPurchased()
                .replace("%prefix%", CobbleMarket.language.getPrefix())
                .replace("%listing_name%", listing.getDisplayName())
                .replace("%price%", MarketUtils.formatPrice(price))
                .replace("%currency%", listing.getCurrency().getCurrency());
        PlayerUtils.sendMessage(player, buyerMsg, null, TypeMessage.CHAT);

        // Notify seller if online
        String sellerMsg = CobbleMarket.language.getMessageListingSold()
                .replace("%prefix%", CobbleMarket.language.getPrefix())
                .replace("%listing_name%", listing.getDisplayName())
                .replace("%price%", MarketUtils.formatPrice(sellerEarnings))
                .replace("%currency%", listing.getCurrency().getCurrency());
        PlayerUtils.sendMessage(listing.getSellerUuid(), sellerMsg, null, TypeMessage.CHAT);

        // Broadcast if enabled
        if (CobbleMarket.config.isBroadcastSales()) {
            String broadcast = CobbleMarket.language.getBroadcastSale()
                    .replace("%buyer%", player.getName().getString())
                    .replace("%seller%", listing.getSellerName())
                    .replace("%listing_name%", listing.getDisplayName())
                    .replace("%price%", MarketUtils.formatPrice(price))
                    .replace("%currency%", listing.getCurrency().getCurrency());
            PlayerUtils.sendMessage(player.getUUID(), broadcast, null, TypeMessage.BROADCAST);
        }

        // Return to market
        MarketMainMenu.open(player);
    }

    private static void cancelListing(ServerPlayer player, Listing<?> listing) {
        // Confirm cancellation
        CobbleMarket.language.getConfirmMenu().open(
                player,
                listing.getDisplayItem(),
                confirmAction -> {
                    // Return item to player
                    if (listing instanceof PokemonListing pokemonListing) {
                        Pokemon pokemon = pokemonListing.getPokemon();
                        if (pokemon != null) {
                            var party = Cobblemon.INSTANCE.getStorage().getParty(player);
                            if (!party.add(pokemon)) {
                                var pc = Cobblemon.INSTANCE.getStorage().getPC(player);
                                if (pc != null) pc.add(pokemon);
                            }
                        }
                    } else if (listing instanceof ItemListing itemListing) {
                        ItemStack item = itemListing.getItemStack();
                        if (!item.isEmpty()) {
                            player.getInventory().add(item);
                        }
                    }

                    // Remove listing
                    CobbleMarket.listingManager.removeListing(listing.getId());

                    PlayerUtils.sendMessage(player, CobbleMarket.language.getMessageListingCancelled()
                            .replace("%prefix%", CobbleMarket.language.getPrefix()), null, TypeMessage.CHAT);
                    MyListingsMenu.open(player);
                },
                cancelAction -> {
                    open(player, listing);
                }
        );
    }
}
