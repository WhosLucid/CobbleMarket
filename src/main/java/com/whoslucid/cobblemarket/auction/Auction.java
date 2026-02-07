package com.whoslucid.cobblemarket.auction;

import com.cobblemon.mod.common.pokemon.Pokemon;
import com.whoslucid.cobblelib.Model.EconomyUse;
import com.whoslucid.cobblemarket.CobbleMarket;
import com.whoslucid.cobblemarket.listing.ListingType;
import com.whoslucid.cobblemarket.listing.PokemonListing;
import lombok.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class Auction extends PokemonListing {

    private BigDecimal startingPrice;
    private BigDecimal currentBid;
    private UUID highestBidderUuid;
    private String highestBidderName;
    private BigDecimal minBidIncrement;
    private List<Bid> bidHistory = new ArrayList<>();

    /**
     * Create a new Auction
     */
    public Auction(UUID sellerUuid, String sellerName, BigDecimal startingPrice,
                   Pokemon pokemon, EconomyUse currency, long durationMillis,
                   BigDecimal minBidIncrement) {
        super(sellerUuid, sellerName, startingPrice, pokemon, currency, durationMillis);
        this.listingType = ListingType.AUCTION;
        this.startingPrice = startingPrice;
        this.currentBid = startingPrice;
        this.minBidIncrement = minBidIncrement != null ? minBidIncrement :
                CobbleMarket.config.getAuctionMinBidIncrement();
        this.bidHistory = new ArrayList<>();
    }

    /**
     * Constructor for deserialization (GSON)
     */
    public Auction() {
        super();
        this.listingType = ListingType.AUCTION;
        this.bidHistory = new ArrayList<>();
    }

    /**
     * Place a bid on this auction
     * @return true if bid was successful, false if bid is too low
     */
    public boolean placeBid(UUID bidderUuid, String bidderName, BigDecimal amount) {
        if (amount.compareTo(getMinNextBid()) < 0) {
            return false;
        }

        // Record the previous highest bidder for refund
        UUID previousBidder = this.highestBidderUuid;

        // Update auction state
        this.highestBidderUuid = bidderUuid;
        this.highestBidderName = bidderName;
        this.currentBid = amount;
        this.price = amount; // Update the listing price to current bid

        // Add to bid history
        if (bidHistory == null) {
            bidHistory = new ArrayList<>();
        }
        bidHistory.add(Bid.create(bidderUuid, bidderName, amount));

        return true;
    }

    /**
     * Get the minimum amount for the next bid
     */
    public BigDecimal getMinNextBid() {
        if (bidHistory == null || bidHistory.isEmpty()) {
            return startingPrice;
        }
        return currentBid.add(minBidIncrement);
    }

    /**
     * Check if there are any bids
     */
    public boolean hasBids() {
        return bidHistory != null && !bidHistory.isEmpty();
    }

    /**
     * Get the number of bids
     */
    public int getBidCount() {
        return bidHistory != null ? bidHistory.size() : 0;
    }

    /**
     * Get the previous highest bidder (for refunds when outbid)
     */
    public Bid getPreviousBid() {
        if (bidHistory == null || bidHistory.size() < 2) {
            return null;
        }
        return bidHistory.get(bidHistory.size() - 2);
    }

    /**
     * Check if the given UUID is the current highest bidder
     */
    public boolean isHighestBidder(UUID uuid) {
        return highestBidderUuid != null && highestBidderUuid.equals(uuid);
    }

    /**
     * Extend the auction by additional time (anti-sniping)
     * Only extends if the auction is ending within a threshold
     */
    public void extendIfEnding(long thresholdMillis, long extensionMillis) {
        if (getRemainingTime() < thresholdMillis && getRemainingTime() > 0) {
            extendDuration(extensionMillis);
        }
    }
}
