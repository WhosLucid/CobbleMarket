package com.whoslucid.cobblemarket.ui;

import ca.landonjw.gooeylibs2.api.UIManager;
import ca.landonjw.gooeylibs2.api.button.Button;
import ca.landonjw.gooeylibs2.api.button.GooeyButton;
import ca.landonjw.gooeylibs2.api.button.PlaceholderButton;
import ca.landonjw.gooeylibs2.api.button.linked.LinkType;
import ca.landonjw.gooeylibs2.api.button.linked.LinkedPageButton;
import ca.landonjw.gooeylibs2.api.helpers.PaginationHelper;
import ca.landonjw.gooeylibs2.api.page.LinkedPage;
import ca.landonjw.gooeylibs2.api.template.types.ChestTemplate;
import com.whoslucid.cobblelib.CobbleLib;
import com.whoslucid.cobblelib.util.AdventureTranslator;
import com.whoslucid.cobblemarket.CobbleMarket;
import com.whoslucid.cobblemarket.config.Lang;
import com.whoslucid.cobblemarket.listing.ItemListing;
import com.whoslucid.cobblemarket.util.MarketUtils;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemLore;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class ItemListingsMenu {

    public static void open(ServerPlayer player) {
        open(player, null);
    }

    public static void open(ServerPlayer player, String searchQuery) {
        CompletableFuture.runAsync(() -> {
            try {
                List<ItemListing> listings = getFilteredListings(searchQuery);
                List<Button> listingButtons = createListingButtons(player, listings);
                Lang.UIButtons buttons = CobbleMarket.language.getButtons();

                // Create template with placeholders for pagination
                PlaceholderButton placeholder = new PlaceholderButton();
                ChestTemplate template = ChestTemplate.builder(6)
                        .rectangle(0, 0, 5, 9, placeholder)  // 5 rows of placeholders for listings
                        .build();

                // Create pagination
                LinkedPage page = PaginationHelper.createPagesFromPlaceholders(
                        template,
                        listingButtons,
                        LinkedPage.builder()
                                .title(AdventureTranslator.toNative(CobbleMarket.language.getTitleItemListings()))
                );

                if (page == null) {
                    page = LinkedPage.builder()
                            .title(AdventureTranslator.toNative(CobbleMarket.language.getTitleItemListings()))
                            .template(template)
                            .build();
                }

                // Add navigation buttons to all pages
                addNavigationButtons(page, player, searchQuery, buttons);

                final LinkedPage finalPage = page;
                player.getServer().execute(() -> UIManager.openUIForcefully(player, finalPage));
            } catch (Exception e) {
                CobbleLib.LOGGER.error("Error opening ItemListingsMenu: " + e.getMessage());
                e.printStackTrace();
            }
        }, CobbleMarket.EXECUTOR);
    }

    private static List<ItemListing> getFilteredListings(String searchQuery) {
        List<ItemListing> allListings = CobbleMarket.listingManager.getItemListings();

        return allListings.stream()
                .filter(listing -> {
                    if (searchQuery != null && !searchQuery.isEmpty()) {
                        return listing.getSearchableText().contains(searchQuery.toLowerCase());
                    }
                    return true;
                })
                .sorted((a, b) -> Long.compare(b.getCreatedTime(), a.getCreatedTime())) // Newest first
                .toList();
    }

    private static List<Button> createListingButtons(ServerPlayer player, List<ItemListing> listings) {
        List<Button> buttons = new ArrayList<>();

        for (ItemListing listing : listings) {
            ItemStack displayItem = listing.getDisplayItem();
            if (displayItem.isEmpty()) continue;

            List<String> lore = new ArrayList<>(CobbleMarket.language.getListingLore());
            List<String> replacedLore = MarketUtils.replaceListing(lore, listing);

            GooeyButton button = GooeyButton.builder()
                    .display(displayItem)
                    .with(DataComponents.CUSTOM_NAME, AdventureTranslator.toNative("&6" + listing.getDisplayName()))
                    .with(DataComponents.LORE, new ItemLore(AdventureTranslator.toNativeL(replacedLore)))
                    .onClick(action -> {
                        ListingDetailMenu.open(player, listing);
                    })
                    .build();

            buttons.add(button);
        }

        return buttons;
    }

    private static void addNavigationButtons(LinkedPage page, ServerPlayer player,
                                              String searchQuery, Lang.UIButtons buttons) {
        LinkedPage current = page;
        while (current != null) {
            ChestTemplate template = (ChestTemplate) current.getTemplate();

            // Fill bottom row with background
            GooeyButton filler = buttons.getFiller().getButton(action -> {});
            for (int i = 45; i < 54; i++) {
                template.set(i, filler);
            }

            // Previous page button
            LinkedPageButton prevButton = LinkedPageButton.builder()
                    .display(buttons.getPreviousPage().getItemStack())
                    .linkType(LinkType.Previous)
                    .build();
            template.set(45, prevButton);

            // Next page button
            LinkedPageButton nextButton = LinkedPageButton.builder()
                    .display(buttons.getNextPage().getItemStack())
                    .linkType(LinkType.Next)
                    .build();
            template.set(53, nextButton);

            // Back button
            GooeyButton backBtn = buttons.getBack().getButton(action -> {
                MarketMainMenu.open(player);
            });
            template.set(49, backBtn);

            current = current.getNext();
        }
    }
}
