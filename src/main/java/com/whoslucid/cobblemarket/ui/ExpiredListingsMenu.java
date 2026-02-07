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
import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.whoslucid.cobblelib.CobbleLib;
import com.whoslucid.cobblelib.util.AdventureTranslator;
import com.whoslucid.cobblelib.util.PlayerUtils;
import com.whoslucid.cobblelib.util.TypeMessage;
import com.whoslucid.cobblemarket.CobbleMarket;
import com.whoslucid.cobblemarket.config.Lang;
import com.whoslucid.cobblemarket.listing.ItemListing;
import com.whoslucid.cobblemarket.listing.Listing;
import com.whoslucid.cobblemarket.listing.PokemonListing;
import com.whoslucid.cobblemarket.util.MarketUtils;
import com.whoslucid.cobblemarket.util.TimeUtils;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemLore;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class ExpiredListingsMenu {

    public static void open(ServerPlayer player) {
        CompletableFuture.runAsync(() -> {
            try {
                List<Listing<?>> listings = CobbleMarket.listingManager.getPlayerExpiredListings(player.getUUID());
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
                                .title(AdventureTranslator.toNative(CobbleMarket.language.getTitleExpiredListings()))
                );

                if (page == null) {
                    page = LinkedPage.builder()
                            .title(AdventureTranslator.toNative(CobbleMarket.language.getTitleExpiredListings()))
                            .template(template)
                            .build();
                }

                // Add navigation buttons
                addNavigationButtons(page, player, buttons);

                final LinkedPage finalPage = page;
                player.getServer().execute(() -> UIManager.openUIForcefully(player, finalPage));
            } catch (Exception e) {
                CobbleLib.LOGGER.error("Error opening ExpiredListingsMenu: " + e.getMessage());
                e.printStackTrace();
            }
        }, CobbleMarket.EXECUTOR);
    }

    private static List<Button> createListingButtons(ServerPlayer player, List<Listing<?>> listings) {
        List<Button> buttons = new ArrayList<>();

        for (Listing<?> listing : listings) {
            ItemStack displayItem = listing.getDisplayItem();
            if (displayItem.isEmpty()) continue;

            List<String> lore = new ArrayList<>();
            lore.add("&7Original Price: &e" + MarketUtils.formatPrice(listing.getPrice()) + " " + listing.getCurrency().getCurrency());
            lore.add("");
            lore.add("&aLeft-click to reclaim");
            lore.add("&eRight-click to relist");

            GooeyButton button = GooeyButton.builder()
                    .display(displayItem)
                    .with(DataComponents.CUSTOM_NAME, AdventureTranslator.toNative("&c" + listing.getDisplayName()))
                    .with(DataComponents.LORE, new ItemLore(AdventureTranslator.toNativeL(lore)))
                    .onClick(action -> {
                        if (action.getClickType() == ca.landonjw.gooeylibs2.api.button.ButtonClick.RIGHT_CLICK ||
                            action.getClickType() == ca.landonjw.gooeylibs2.api.button.ButtonClick.SHIFT_RIGHT_CLICK) {
                            // Relist
                            relistItem(player, listing);
                        } else {
                            // Reclaim
                            reclaimItem(player, listing);
                        }
                    })
                    .build();

            buttons.add(button);
        }

        return buttons;
    }

    private static void reclaimItem(ServerPlayer player, Listing<?> listing) {
        Listing<?> reclaimed = CobbleMarket.listingManager.reclaimExpired(player.getUUID(), listing.getId());
        if (reclaimed == null) {
            PlayerUtils.sendMessage(player, CobbleMarket.language.getMessageListingNotFound()
                    .replace("%prefix%", CobbleMarket.language.getPrefix()), null, TypeMessage.CHAT);
            open(player);
            return;
        }

        // Return item
        if (reclaimed instanceof PokemonListing pokemonListing) {
            Pokemon pokemon = pokemonListing.getPokemon();
            if (pokemon != null) {
                var party = Cobblemon.INSTANCE.getStorage().getParty(player);
                if (!party.add(pokemon)) {
                    var pc = Cobblemon.INSTANCE.getStorage().getPC(player);
                    if (pc != null) pc.add(pokemon);
                }
            }
        } else if (reclaimed instanceof ItemListing itemListing) {
            ItemStack item = itemListing.getItemStack();
            if (!item.isEmpty()) {
                if (!player.getInventory().add(item)) {
                    player.drop(item, false);
                }
            }
        }

        PlayerUtils.sendMessage(player, CobbleMarket.language.getMessageListingReclaimed()
                        .replace("%prefix%", CobbleMarket.language.getPrefix())
                        .replace("%listing_name%", listing.getDisplayName()),
                null, TypeMessage.CHAT);
        open(player);
    }

    private static void relistItem(ServerPlayer player, Listing<?> listing) {
        // Check listing limit
        if (MarketUtils.hasReachedListingLimit(player.getUUID())) {
            PlayerUtils.sendMessage(player, CobbleMarket.language.getMessageMaxListingsReached()
                            .replace("%prefix%", CobbleMarket.language.getPrefix())
                            .replace("%max%", String.valueOf(CobbleMarket.config.getMaxListingsPerPlayer())),
                    null, TypeMessage.CHAT);
            return;
        }

        long duration = TimeUtils.hoursToMillis(CobbleMarket.config.getListingDurationHours());
        CobbleMarket.listingManager.relist(listing, duration);

        PlayerUtils.sendMessage(player, CobbleMarket.language.getMessageListingRelisted()
                        .replace("%prefix%", CobbleMarket.language.getPrefix())
                        .replace("%listing_name%", listing.getDisplayName()),
                null, TypeMessage.CHAT);
        open(player);
    }

    private static void addNavigationButtons(LinkedPage page, ServerPlayer player, Lang.UIButtons buttons) {
        LinkedPage current = page;
        while (current != null) {
            ChestTemplate template = (ChestTemplate) current.getTemplate();

            // Fill bottom row
            GooeyButton filler = buttons.getFiller().getButton(action -> {});
            for (int i = 45; i < 54; i++) {
                template.set(i, filler);
            }

            // Previous page
            LinkedPageButton prevButton = LinkedPageButton.builder()
                    .display(buttons.getPreviousPage().getItemStack())
                    .linkType(LinkType.Previous)
                    .build();
            template.set(45, prevButton);

            // Next page
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
