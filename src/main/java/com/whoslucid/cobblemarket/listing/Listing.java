package com.whoslucid.cobblemarket.listing;

import com.whoslucid.cobblelib.Model.EconomyUse;
import com.whoslucid.cobblemarket.util.TimeUtils;
import lombok.*;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@EqualsAndHashCode
@ToString
public abstract class Listing<T> {
    protected UUID id;
    protected UUID sellerUuid;
    protected String sellerName;
    protected BigDecimal price;
    protected long createdTime;
    protected long endTime;
    protected ListingType listingType;
    protected EconomyUse currency;
    protected boolean isPokemon;

    // Version for serialization compatibility
    protected String version = "1.0";

    /**
     * Get the actual listed item (Pokemon or ItemStack)
     */
    public abstract T getItem();

    /**
     * Get an ItemStack representation for display in UI
     */
    public abstract ItemStack getDisplayItem();

    /**
     * Get the display name of the listing
     */
    public abstract String getDisplayName();

    /**
     * Get searchable text for filtering
     */
    public abstract String getSearchableText();

    /**
     * Validate that the listing data is intact
     */
    public abstract boolean isValid();

    /**
     * Check if the listing has expired
     */
    public boolean isExpired() {
        if (endTime <= 0) {
            return false; // No expiration (-1 means infinite)
        }
        return System.currentTimeMillis() > endTime;
    }

    /**
     * Get remaining time in milliseconds
     */
    public long getRemainingTime() {
        if (endTime <= 0) {
            return Long.MAX_VALUE; // Infinite
        }
        return Math.max(0, endTime - System.currentTimeMillis());
    }

    /**
     * Get formatted remaining time
     */
    public String getFormattedRemainingTime() {
        if (endTime <= 0) {
            return "No Expiry";
        }
        return TimeUtils.formatDuration(getRemainingTime());
    }

    /**
     * Check if this is an auction listing
     */
    public boolean isAuction() {
        return listingType == ListingType.AUCTION;
    }

    /**
     * Check if the seller matches the given UUID
     */
    public boolean isSeller(UUID uuid) {
        return sellerUuid != null && sellerUuid.equals(uuid);
    }

    /**
     * Extend the listing duration by the given milliseconds
     */
    public void extendDuration(long additionalMillis) {
        if (endTime > 0) {
            endTime += additionalMillis;
        }
    }

    /**
     * Reset the listing end time based on current time and given duration
     */
    public void resetDuration(long durationMillis) {
        if (durationMillis > 0) {
            endTime = System.currentTimeMillis() + durationMillis;
        } else {
            endTime = -1; // No expiration
        }
    }
}
