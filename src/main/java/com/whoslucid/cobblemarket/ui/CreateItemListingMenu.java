package com.whoslucid.cobblemarket.ui;

import ca.landonjw.gooeylibs2.api.UIManager;
import ca.landonjw.gooeylibs2.api.button.Button;
import ca.landonjw.gooeylibs2.api.button.GooeyButton;
import ca.landonjw.gooeylibs2.api.page.GooeyPage;
import ca.landonjw.gooeylibs2.api.template.types.ChestTemplate;
import com.whoslucid.cobblelib.CobbleLib;
import com.whoslucid.cobblelib.Model.EconomyUse;
import com.whoslucid.cobblelib.util.AdventureTranslator;
import com.whoslucid.cobblelib.util.PlayerUtils;
import com.whoslucid.cobblelib.util.TypeMessage;
import com.whoslucid.cobblemarket.CobbleMarket;
import com.whoslucid.cobblemarket.config.Lang;
import com.whoslucid.cobblemarket.listing.ItemListing;
import com.whoslucid.cobblemarket.util.MarketUtils;
import com.whoslucid.cobblemarket.util.TimeUtils;
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

public class CreateItemListingMenu {

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

        CompletableFuture.runAsync(() -> {
            try {
                ChestTemplate template = buildInventoryTemplate(player);

                GooeyPage page = GooeyPage.builder()
                        .template(template)
                        .title(AdventureTranslator.toNative("&0Select Item to List"))
                        .build();

                player.getServer().execute(() -> UIManager.openUIForcefully(player, page));
            } catch (Exception e) {
                CobbleLib.LOGGER.error("Error opening CreateItemListingMenu: " + e.getMessage());
                e.printStackTrace();
            }
        }, CobbleMarket.EXECUTOR);
    }

    private static ChestTemplate buildInventoryTemplate(ServerPlayer player) {
        ChestTemplate.Builder builder = ChestTemplate.builder(6);
        Lang.UIButtons buttons = CobbleMarket.language.getButtons();

        // Fill background
        GooeyButton filler = buttons.getFiller().getButton(action -> {});
        for (int i = 0; i < 54; i++) {
            builder.set(i, filler);
        }

        // Display player's inventory items (slots 0-35 in player inventory)
        int guiSlot = 0;
        for (int invSlot = 0; invSlot < 36 && guiSlot < 45; invSlot++) {
            ItemStack stack = player.getInventory().getItem(invSlot);
            if (stack.isEmpty()) continue;

            // Skip blacklisted items
            if (MarketUtils.isItemBlacklisted(stack)) continue;

            final int finalInvSlot = invSlot;
            final ItemStack displayStack = stack.copy();

            List<String> lore = new ArrayList<>();
            lore.add("");
            lore.add("&7Amount: &e" + stack.getCount());
            lore.add("");
            lore.add("&aClick to list this item");

            GooeyButton itemBtn = GooeyButton.builder()
                    .display(displayStack)
                    .with(DataComponents.LORE, new ItemLore(AdventureTranslator.toNativeL(lore)))
                    .onClick(action -> {
                        openPriceSelection(player, finalInvSlot);
                    })
                    .build();

            builder.set(guiSlot++, itemBtn);
        }

        // Back button
        GooeyButton backBtn = buttons.getBack().getButton(action -> {
            MarketMainMenu.open(player);
        });
        builder.set(49, backBtn);

        return builder.build();
    }

    public static void openPriceSelection(ServerPlayer player, int inventorySlot) {
        ItemStack stack = player.getInventory().getItem(inventorySlot);
        if (stack.isEmpty()) {
            PlayerUtils.sendMessage(player, "&cItem not found in inventory", null, TypeMessage.CHAT);
            open(player);
            return;
        }

        // Check if item is blacklisted
        if (MarketUtils.isItemBlacklisted(stack)) {
            PlayerUtils.sendMessage(player, CobbleMarket.language.getMessageItemBlacklisted()
                    .replace("%prefix%", CobbleMarket.language.getPrefix()), null, TypeMessage.CHAT);
            open(player);
            return;
        }

        CompletableFuture.runAsync(() -> {
            try {
                BigDecimal minPrice = CobbleMarket.config.getMinimumPrice();
                BigDecimal suggestedPrice = minPrice.multiply(BigDecimal.valueOf(stack.getCount()));

                ChestTemplate.Builder builder = ChestTemplate.builder(3);
                Lang.UIButtons buttons = CobbleMarket.language.getButtons();

                // Fill background
                GooeyButton filler = buttons.getFiller().getButton(action -> {});
                for (int i = 0; i < 27; i++) {
                    builder.set(i, filler);
                }

                // Display item info
                ItemStack displayStack = stack.copy();
                List<String> itemLore = Arrays.asList(
                        "&7Amount: &e" + stack.getCount(),
                        "",
                        "&7Minimum Price: &e" + MarketUtils.formatPrice(minPrice),
                        "",
                        "&eClick a price button to list"
                );

                GooeyButton itemBtn = GooeyButton.builder()
                        .display(displayStack)
                        .with(DataComponents.LORE, new ItemLore(AdventureTranslator.toNativeL(itemLore)))
                        .build();
                builder.set(13, itemBtn);

                // Price buttons
                BigDecimal price1 = suggestedPrice.max(minPrice);
                BigDecimal price2 = suggestedPrice.multiply(BigDecimal.valueOf(2)).max(minPrice);
                BigDecimal price3 = suggestedPrice.multiply(BigDecimal.valueOf(5)).max(minPrice);

                builder.set(10, createPriceButton(player, inventorySlot, price1, "&a"));
                builder.set(11, createPriceButton(player, inventorySlot, price2, "&e"));
                builder.set(12, createPriceButton(player, inventorySlot, price3, "&6"));

                // Custom price button
                GooeyButton customBtn = GooeyButton.builder()
                        .display(new ItemStack(Items.NAME_TAG))
                        .with(DataComponents.CUSTOM_NAME, AdventureTranslator.toNative("&bCustom Price"))
                        .with(DataComponents.LORE, new ItemLore(AdventureTranslator.toNativeL(Arrays.asList(
                                "&7Use command:",
                                "&e/market listitem <price>"
                        ))))
                        .build();
                builder.set(16, customBtn);

                // Back button
                GooeyButton backBtn = buttons.getBack().getButton(action -> {
                    open(player);
                });
                builder.set(22, backBtn);

                GooeyPage page = GooeyPage.builder()
                        .template(builder.build())
                        .title(AdventureTranslator.toNative("&0Set Item Price"))
                        .build();

                player.getServer().execute(() -> UIManager.openUIForcefully(player, page));
            } catch (Exception e) {
                CobbleLib.LOGGER.error("Error opening item price selection: " + e.getMessage());
                e.printStackTrace();
            }
        }, CobbleMarket.EXECUTOR);
    }

    private static GooeyButton createPriceButton(ServerPlayer player, int inventorySlot, BigDecimal price, String color) {
        return GooeyButton.builder()
                .display(new ItemStack(Items.EMERALD))
                .with(DataComponents.CUSTOM_NAME, AdventureTranslator.toNative(color + MarketUtils.formatPrice(price) + " " +
                        CobbleMarket.config.getDefaultCurrency().getCurrency()))
                .with(DataComponents.LORE, new ItemLore(AdventureTranslator.toNativeL(Arrays.asList(
                        "&7Click to list for this price"
                ))))
                .onClick(action -> {
                    createListing(player, inventorySlot, price);
                })
                .build();
    }

    public static void createListing(ServerPlayer player, int inventorySlot, BigDecimal price) {
        ItemStack stack = player.getInventory().getItem(inventorySlot);
        if (stack.isEmpty()) {
            PlayerUtils.sendMessage(player, "&cItem not found in inventory", null, TypeMessage.CHAT);
            return;
        }

        // Final checks
        if (MarketUtils.isItemBlacklisted(stack)) {
            PlayerUtils.sendMessage(player, CobbleMarket.language.getMessageItemBlacklisted()
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

        BigDecimal minPrice = CobbleMarket.config.getMinimumPrice();
        if (price.compareTo(minPrice) < 0) {
            PlayerUtils.sendMessage(player, CobbleMarket.language.getMessagePriceTooLow()
                            .replace("%prefix%", CobbleMarket.language.getPrefix())
                            .replace("%min_price%", MarketUtils.formatPrice(minPrice))
                            .replace("%currency%", CobbleMarket.config.getDefaultCurrency().getCurrency()),
                    null, TypeMessage.CHAT);
            return;
        }

        // Remove item from inventory
        ItemStack listingStack = stack.copy();
        player.getInventory().setItem(inventorySlot, ItemStack.EMPTY);

        // Create listing
        EconomyUse currency = CobbleMarket.config.getDefaultCurrency();
        long duration = TimeUtils.hoursToMillis(CobbleMarket.config.getListingDurationHours());

        ItemListing listing = new ItemListing(
                player.getUUID(),
                player.getName().getString(),
                price,
                listingStack,
                currency,
                duration
        );

        CobbleMarket.listingManager.addListing(listing);

        // Send success message
        String itemName = listingStack.getHoverName().getString();
        String msg = CobbleMarket.language.getMessageListingCreated()
                .replace("%prefix%", CobbleMarket.language.getPrefix())
                .replace("%price%", MarketUtils.formatPrice(price))
                .replace("%currency%", currency.getCurrency());
        PlayerUtils.sendMessage(player, msg, null, TypeMessage.CHAT);

        // Broadcast if enabled
        if (CobbleMarket.config.isBroadcastNewListings()) {
            String broadcast = CobbleMarket.language.getBroadcastNewListing()
                    .replace("%seller%", player.getName().getString())
                    .replace("%listing_name%", itemName)
                    .replace("%price%", MarketUtils.formatPrice(price))
                    .replace("%currency%", currency.getCurrency());
            PlayerUtils.sendMessage(player.getUUID(), broadcast, null, TypeMessage.BROADCAST);
        }

        MarketMainMenu.open(player);
    }
}
