package com.whoslucid.cobblemarket.ui;

import ca.landonjw.gooeylibs2.api.UIManager;
import ca.landonjw.gooeylibs2.api.button.GooeyButton;
import ca.landonjw.gooeylibs2.api.page.GooeyPage;
import ca.landonjw.gooeylibs2.api.template.types.ChestTemplate;
import com.whoslucid.cobblelib.Model.ItemModel;
import com.whoslucid.cobblelib.util.AdventureTranslator;
import com.whoslucid.cobblemarket.CobbleMarket;
import com.whoslucid.cobblemarket.config.Lang;
import net.minecraft.server.level.ServerPlayer;

import java.util.concurrent.CompletableFuture;

public class MarketMainMenu {

    public static void open(ServerPlayer player) {
        CompletableFuture.runAsync(() -> {
            ChestTemplate template = buildTemplate(player);
            GooeyPage page = GooeyPage.builder()
                    .template(template)
                    .title(AdventureTranslator.toNative(CobbleMarket.language.getTitleMainMenu()))
                    .build();

            player.getServer().execute(() -> UIManager.openUIForcefully(player, page));
        }, CobbleMarket.EXECUTOR);
    }

    private static ChestTemplate buildTemplate(ServerPlayer player) {
        ChestTemplate.Builder builder = ChestTemplate.builder(6);
        Lang.UIButtons buttons = CobbleMarket.language.getButtons();

        // Fill background
        GooeyButton filler = buttons.getFiller().getButton(action -> {});
        for (int i = 0; i < 54; i++) {
            builder.set(i, filler);
        }

        // Pokemon Listings Button
        if (CobbleMarket.config.isEnablePokemonSales()) {
            GooeyButton pokemonBtn = buttons.getPokemonListings().getButton(action -> {
                PokemonListingsMenu.open(player);
            });
            builder.set(buttons.getPokemonListings().getSlot(), pokemonBtn);
        }

        // Item Listings Button
        if (CobbleMarket.config.isEnableItemSales()) {
            GooeyButton itemsBtn = buttons.getItemListings().getButton(action -> {
                ItemListingsMenu.open(player);
            });
            builder.set(buttons.getItemListings().getSlot(), itemsBtn);
        }

        // Auctions Button
        if (CobbleMarket.config.isEnableAuctions()) {
            GooeyButton auctionsBtn = buttons.getAuctions().getButton(action -> {
                AuctionsMenu.open(player);
            });
            builder.set(buttons.getAuctions().getSlot(), auctionsBtn);
        }

        // My Listings Button
        GooeyButton myListingsBtn = buttons.getMyListings().getButton(action -> {
            MyListingsMenu.open(player);
        });
        builder.set(buttons.getMyListings().getSlot(), myListingsBtn);

        // Expired Listings Button
        GooeyButton expiredBtn = buttons.getExpiredListings().getButton(action -> {
            ExpiredListingsMenu.open(player);
        });
        builder.set(buttons.getExpiredListings().getSlot(), expiredBtn);

        // History Button
        GooeyButton historyBtn = buttons.getHistory().getButton(action -> {
            HistoryMenu.open(player);
        });
        builder.set(buttons.getHistory().getSlot(), historyBtn);

        // Create Listing Button
        GooeyButton createBtn = buttons.getCreateListing().getButton(action -> {
            CreateListingMenu.open(player);
        });
        builder.set(buttons.getCreateListing().getSlot(), createBtn);

        // Close Button
        GooeyButton closeBtn = buttons.getClose().getButton(action -> {
            action.getPlayer().closeContainer();
        });
        builder.set(49, closeBtn);

        return builder.build();
    }
}
