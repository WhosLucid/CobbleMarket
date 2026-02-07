package com.whoslucid.cobblemarket.auction;

import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.whoslucid.cobblelib.CobbleLib;
import com.whoslucid.cobblelib.api.EconomyApi;
import com.whoslucid.cobblelib.util.PlayerUtils;
import com.whoslucid.cobblelib.util.TypeMessage;
import com.whoslucid.cobblemarket.CobbleMarket;
import com.whoslucid.cobblemarket.history.TransactionRecord;
import com.whoslucid.cobblemarket.listing.ListingType;
import com.whoslucid.cobblemarket.util.MarketUtils;
import com.whoslucid.cobblemarket.util.PriceCalculator;
import net.minecraft.server.level.ServerPlayer;

import java.math.BigDecimal;
import java.util.UUID;

public class AuctionManager {

    /**
     * Process an auction that has ended
     */
    public static void processAuctionEnd(Auction auction) {
        if (auction.hasBids()) {
            processAuctionWithWinner(auction);
        } else {
            processAuctionNoBids(auction);
        }
    }

    /**
     * Process auction with a winner
     */
    private static void processAuctionWithWinner(Auction auction) {
        UUID winnerId = auction.getHighestBidderUuid();
        String winnerName = auction.getHighestBidderName();
        BigDecimal finalPrice = auction.getCurrentBid();

        // Winner already paid when placing bid, so just transfer the Pokemon
        Pokemon pokemon = auction.getPokemon();
        if (pokemon == null) {
            // Refund winner
            EconomyApi.addMoney(winnerId, finalPrice, auction.getCurrency());
            return;
        }

        // Try to give Pokemon to winner
        ServerPlayer winner = CobbleMarket.server.getPlayerList().getPlayer(winnerId);
        if (winner != null) {
            // Winner is online
            var party = Cobblemon.INSTANCE.getStorage().getParty(winner);
            if (!party.add(pokemon)) {
                var pc = Cobblemon.INSTANCE.getStorage().getPC(winner);
                if (pc != null) {
                    pc.add(pokemon);
                }
            }
        } else {
            // Winner is offline - refund and they'll need to reclaim
            // We can't access PC for offline players without a ServerPlayer
            EconomyApi.addMoney(winnerId, finalPrice, auction.getCurrency());
            CobbleLib.LOGGER.info("Auction winner " + winnerName + " was offline. Refunding bid.");
            return;
        }

        // Pay seller (minus tax)
        BigDecimal sellerEarnings = PriceCalculator.calculateSellerEarnings(finalPrice);
        EconomyApi.addMoney(auction.getSellerUuid(), sellerEarnings, auction.getCurrency());

        // Record transactions
        BigDecimal tax = PriceCalculator.calculateTax(finalPrice);
        CobbleMarket.historyManager.addTransaction(winnerId,
                TransactionRecord.purchase(auction.getDisplayName(), finalPrice, auction.getCurrency().getCurrency(),
                        auction.getSellerName(), auction.getSellerUuid(), true, ListingType.AUCTION));

        CobbleMarket.historyManager.addTransaction(auction.getSellerUuid(),
                TransactionRecord.sale(auction.getDisplayName(), finalPrice, auction.getCurrency().getCurrency(),
                        tax, winnerName, winnerId, true, ListingType.AUCTION));

        // Notify winner
        PlayerUtils.sendMessage(winnerId, CobbleMarket.language.getMessageAuctionWon()
                        .replace("%prefix%", CobbleMarket.language.getPrefix())
                        .replace("%listing_name%", auction.getDisplayName()),
                null, TypeMessage.CHAT);

        // Notify seller
        PlayerUtils.sendMessage(auction.getSellerUuid(), CobbleMarket.language.getMessageListingSold()
                        .replace("%prefix%", CobbleMarket.language.getPrefix())
                        .replace("%listing_name%", auction.getDisplayName())
                        .replace("%price%", MarketUtils.formatPrice(sellerEarnings))
                        .replace("%currency%", auction.getCurrency().getCurrency()),
                null, TypeMessage.CHAT);

        // Broadcast
        if (CobbleMarket.config.isBroadcastSales()) {
            String broadcast = CobbleMarket.language.getBroadcastAuctionEnd()
                    .replace("%listing_name%", auction.getDisplayName())
                    .replace("%winner%", winnerName)
                    .replace("%price%", MarketUtils.formatPrice(finalPrice))
                    .replace("%currency%", auction.getCurrency().getCurrency());
            PlayerUtils.sendMessage(winnerId, broadcast, null, TypeMessage.BROADCAST);
        }
    }

    /**
     * Process auction with no bids
     */
    private static void processAuctionNoBids(Auction auction) {
        // Notify seller
        PlayerUtils.sendMessage(auction.getSellerUuid(), CobbleMarket.language.getMessageAuctionEndedNoBids()
                        .replace("%prefix%", CobbleMarket.language.getPrefix())
                        .replace("%listing_name%", auction.getDisplayName()),
                null, TypeMessage.CHAT);
    }
}
