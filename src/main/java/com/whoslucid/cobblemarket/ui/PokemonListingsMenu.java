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
import com.whoslucid.cobblemarket.listing.PokemonListing;
import com.whoslucid.cobblemarket.util.MarketUtils;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemLore;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class PokemonListingsMenu {

    public enum Filter {
        ALL,
        SHINY,
        LEGENDARY
    }

    public static void open(ServerPlayer player) {
        open(player, Filter.ALL, null);
    }

    public static void openWithSearch(ServerPlayer player, String searchQuery) {
        open(player, Filter.ALL, searchQuery);
    }

    public static void open(ServerPlayer player, Filter filter, String searchQuery) {
        CompletableFuture.runAsync(() -> {
            try {
                List<PokemonListing> listings = getFilteredListings(filter, searchQuery);
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
                                .title(AdventureTranslator.toNative(CobbleMarket.language.getTitlePokemonListings()))
                );

                if (page == null) {
                    // Empty listings - create a basic page
                    page = LinkedPage.builder()
                            .title(AdventureTranslator.toNative(CobbleMarket.language.getTitlePokemonListings()))
                            .template(template)
                            .build();
                }

                // Add navigation buttons to all pages
                addNavigationButtons(page, player, filter, searchQuery, buttons);

                final LinkedPage finalPage = page;
                player.getServer().execute(() -> UIManager.openUIForcefully(player, finalPage));
            } catch (Exception e) {
                CobbleLib.LOGGER.error("Error opening PokemonListingsMenu: " + e.getMessage());
                e.printStackTrace();
            }
        }, CobbleMarket.EXECUTOR);
    }

    private static List<PokemonListing> getFilteredListings(Filter filter, String searchQuery) {
        List<PokemonListing> allListings = CobbleMarket.listingManager.getPokemonListings();

        return allListings.stream()
                .filter(listing -> {
                    // Apply filter
                    if (filter == Filter.SHINY && !listing.isShiny()) return false;
                    if (filter == Filter.LEGENDARY && !listing.isLegendary()) return false;

                    // Apply search
                    if (searchQuery != null && !searchQuery.isEmpty()) {
                        return listing.getSearchableText().contains(searchQuery.toLowerCase());
                    }
                    return true;
                })
                .sorted((a, b) -> Long.compare(b.getCreatedTime(), a.getCreatedTime())) // Newest first
                .toList();
    }

    private static List<Button> createListingButtons(ServerPlayer player, List<PokemonListing> listings) {
        List<Button> buttons = new ArrayList<>();

        for (PokemonListing listing : listings) {
            ItemStack displayItem = listing.getDisplayItem();
            if (displayItem.isEmpty()) continue;

            List<String> lore = new ArrayList<>();
            lore.addAll(CobbleMarket.language.getPokemonLore());
            lore.add("");
            lore.addAll(CobbleMarket.language.getListingLore());

            List<String> replacedLore = MarketUtils.replaceListing(lore, listing);

            GooeyButton button = GooeyButton.builder()
                    .display(displayItem)
                    .with(DataComponents.CUSTOM_NAME, AdventureTranslator.toNative("&b" + listing.getDisplayName()))
                    .with(DataComponents.LORE, new ItemLore(AdventureTranslator.toNativeL(replacedLore)))
                    .onClick(action -> {
                        ListingDetailMenu.open(player, listing);
                    })
                    .build();

            buttons.add(button);
        }

        return buttons;
    }

    private static void addNavigationButtons(LinkedPage page, ServerPlayer player, Filter currentFilter,
                                              String searchQuery, Lang.UIButtons buttons) {
        // Traverse all pages and add buttons
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

            // Filter buttons
            String shinyTitle = currentFilter == Filter.SHINY ? "&e&lShiny Only (Active)" : buttons.getFilterShiny().getDisplayname();
            GooeyButton shinyFilter = GooeyButton.builder()
                    .display(buttons.getFilterShiny().getItemStack())
                    .with(DataComponents.CUSTOM_NAME, AdventureTranslator.toNative(shinyTitle))
                    .with(DataComponents.LORE, new ItemLore(AdventureTranslator.toNativeL(buttons.getFilterShiny().getLore())))
                    .onClick(action -> {
                        Filter newFilter = currentFilter == Filter.SHINY ? Filter.ALL : Filter.SHINY;
                        open(player, newFilter, searchQuery);
                    })
                    .build();
            template.set(48, shinyFilter);

            GooeyButton allFilter = GooeyButton.builder()
                    .display(buttons.getFilterAll().getItemStack())
                    .with(DataComponents.CUSTOM_NAME, AdventureTranslator.toNative(buttons.getFilterAll().getDisplayname()))
                    .with(DataComponents.LORE, new ItemLore(AdventureTranslator.toNativeL(buttons.getFilterAll().getLore())))
                    .onClick(action -> {
                        open(player, Filter.ALL, null);
                    })
                    .build();
            template.set(49, allFilter);

            String legendaryTitle = currentFilter == Filter.LEGENDARY ? "&5&lLegendary Only (Active)" : buttons.getFilterLegendary().getDisplayname();
            GooeyButton legendaryFilter = GooeyButton.builder()
                    .display(buttons.getFilterLegendary().getItemStack())
                    .with(DataComponents.CUSTOM_NAME, AdventureTranslator.toNative(legendaryTitle))
                    .with(DataComponents.LORE, new ItemLore(AdventureTranslator.toNativeL(buttons.getFilterLegendary().getLore())))
                    .onClick(action -> {
                        Filter newFilter = currentFilter == Filter.LEGENDARY ? Filter.ALL : Filter.LEGENDARY;
                        open(player, newFilter, searchQuery);
                    })
                    .build();
            template.set(50, legendaryFilter);

            // Back button
            GooeyButton backBtn = buttons.getBack().getButton(action -> {
                MarketMainMenu.open(player);
            });
            template.set(46, backBtn);

            current = current.getNext();
        }
    }
}
