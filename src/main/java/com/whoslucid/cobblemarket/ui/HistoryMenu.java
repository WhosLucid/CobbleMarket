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
import com.whoslucid.cobblemarket.history.PlayerHistory;
import com.whoslucid.cobblemarket.history.TransactionRecord;
import com.whoslucid.cobblemarket.util.MarketUtils;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemLore;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class HistoryMenu {

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("MM/dd/yyyy HH:mm");

    public static void open(ServerPlayer player) {
        CompletableFuture.runAsync(() -> {
            try {
                PlayerHistory history = CobbleMarket.historyManager.getOrCreateHistory(player.getUUID());
                List<TransactionRecord> transactions = history.getTransactionsSorted();
                List<Button> buttons = createTransactionButtons(transactions);
                Lang.UIButtons uiButtons = CobbleMarket.language.getButtons();

                // Create template with placeholders for pagination
                PlaceholderButton placeholder = new PlaceholderButton();
                ChestTemplate template = ChestTemplate.builder(6)
                        .rectangle(0, 0, 5, 9, placeholder)  // 5 rows of placeholders for listings
                        .build();

                // Create pagination
                LinkedPage page = PaginationHelper.createPagesFromPlaceholders(
                        template,
                        buttons,
                        LinkedPage.builder()
                                .title(AdventureTranslator.toNative(CobbleMarket.language.getTitleHistory()))
                );

                if (page == null) {
                    page = LinkedPage.builder()
                            .title(AdventureTranslator.toNative(CobbleMarket.language.getTitleHistory()))
                            .template(template)
                            .build();
                }

                // Add navigation buttons
                addNavigationButtons(page, player, uiButtons);

                final LinkedPage finalPage = page;
                player.getServer().execute(() -> UIManager.openUIForcefully(player, finalPage));
            } catch (Exception e) {
                CobbleLib.LOGGER.error("Error opening HistoryMenu: " + e.getMessage());
                e.printStackTrace();
            }
        }, CobbleMarket.EXECUTOR);
    }

    private static List<Button> createTransactionButtons(List<TransactionRecord> transactions) {
        List<Button> buttons = new ArrayList<>();

        for (TransactionRecord record : transactions) {
            ItemStack displayItem = getDisplayItem(record);
            String titleColor = record.getTransactionType() == TransactionRecord.TransactionType.SALE ||
                    record.getTransactionType() == TransactionRecord.TransactionType.AUCTION_SOLD
                    ? "&a" : "&e";

            List<String> lore = new ArrayList<>();
            lore.add("&7Type: &f" + formatTransactionType(record.getTransactionType()));
            lore.add("&7Price: &e" + MarketUtils.formatPrice(record.getPrice()) + " " + record.getCurrency());
            if (record.getTaxDeducted() != null && record.getTaxDeducted().compareTo(java.math.BigDecimal.ZERO) > 0) {
                lore.add("&7Tax: &c-" + MarketUtils.formatPrice(record.getTaxDeducted()));
            }
            lore.add("&7Other Party: &f" + record.getOtherPartyName());
            lore.add("&7Date: &f" + DATE_FORMAT.format(new Date(record.getTimestamp())));

            GooeyButton button = GooeyButton.builder()
                    .display(displayItem)
                    .with(DataComponents.CUSTOM_NAME, AdventureTranslator.toNative(titleColor + record.getItemName()))
                    .with(DataComponents.LORE, new ItemLore(AdventureTranslator.toNativeL(lore)))
                    .build();

            buttons.add(button);
        }

        return buttons;
    }

    private static ItemStack getDisplayItem(TransactionRecord record) {
        // Use paper with different colors based on transaction type
        return switch (record.getTransactionType()) {
            case SALE, AUCTION_SOLD -> new ItemStack(Items.EMERALD);
            case PURCHASE, AUCTION_WIN -> new ItemStack(Items.GOLD_INGOT);
        };
    }

    private static String formatTransactionType(TransactionRecord.TransactionType type) {
        return switch (type) {
            case SALE -> "Sale";
            case PURCHASE -> "Purchase";
            case AUCTION_WIN -> "Auction Won";
            case AUCTION_SOLD -> "Auction Sold";
        };
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
