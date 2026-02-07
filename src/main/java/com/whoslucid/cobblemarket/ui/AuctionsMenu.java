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
import com.whoslucid.cobblemarket.auction.Auction;
import com.whoslucid.cobblemarket.config.Lang;
import com.whoslucid.cobblemarket.util.MarketUtils;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemLore;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class AuctionsMenu {

    public static void open(ServerPlayer player) {
        CompletableFuture.runAsync(() -> {
            try {
                List<Auction> auctions = CobbleMarket.listingManager.getAuctions();
                List<Button> auctionButtons = createAuctionButtons(player, auctions);
                Lang.UIButtons buttons = CobbleMarket.language.getButtons();

                // Create template with placeholders for pagination
                PlaceholderButton placeholder = new PlaceholderButton();
                ChestTemplate template = ChestTemplate.builder(6)
                        .rectangle(0, 0, 5, 9, placeholder)  // 5 rows of placeholders for listings
                        .build();

                // Create pagination
                LinkedPage page = PaginationHelper.createPagesFromPlaceholders(
                        template,
                        auctionButtons,
                        LinkedPage.builder()
                                .title(AdventureTranslator.toNative(CobbleMarket.language.getTitleAuctions()))
                );

                if (page == null) {
                    page = LinkedPage.builder()
                            .title(AdventureTranslator.toNative(CobbleMarket.language.getTitleAuctions()))
                            .template(template)
                            .build();
                }

                // Add navigation buttons
                addNavigationButtons(page, player, buttons);

                final LinkedPage finalPage = page;
                player.getServer().execute(() -> UIManager.openUIForcefully(player, finalPage));
            } catch (Exception e) {
                CobbleLib.LOGGER.error("Error opening AuctionsMenu: " + e.getMessage());
                e.printStackTrace();
            }
        }, CobbleMarket.EXECUTOR);
    }

    private static List<Button> createAuctionButtons(ServerPlayer player, List<Auction> auctions) {
        List<Button> buttons = new ArrayList<>();

        for (Auction auction : auctions) {
            ItemStack displayItem = auction.getDisplayItem();
            if (displayItem.isEmpty()) continue;

            List<String> lore = new ArrayList<>();
            lore.addAll(CobbleMarket.language.getPokemonLore());
            lore.add("");
            lore.addAll(CobbleMarket.language.getAuctionLore());

            // Replace auction-specific placeholders
            List<String> replacedLore = new ArrayList<>();
            for (String line : lore) {
                line = MarketUtils.replaceListing(line, auction);
                line = line.replace("%current_bid%", MarketUtils.formatPrice(auction.getCurrentBid()));
                line = line.replace("%bidder%", auction.getHighestBidderName() != null ?
                        auction.getHighestBidderName() : "No bids");
                line = line.replace("%min_next_bid%", MarketUtils.formatPrice(auction.getMinNextBid()));
                replacedLore.add(line);
            }

            GooeyButton button = GooeyButton.builder()
                    .display(displayItem)
                    .with(DataComponents.CUSTOM_NAME, AdventureTranslator.toNative("&6" + auction.getDisplayName()))
                    .with(DataComponents.LORE, new ItemLore(AdventureTranslator.toNativeL(replacedLore)))
                    .onClick(action -> {
                        AuctionDetailMenu.open(player, auction);
                    })
                    .build();

            buttons.add(button);
        }

        return buttons;
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
