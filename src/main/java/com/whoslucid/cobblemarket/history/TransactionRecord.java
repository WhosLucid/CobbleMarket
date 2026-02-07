package com.whoslucid.cobblemarket.history;

import com.whoslucid.cobblemarket.listing.ListingType;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@EqualsAndHashCode
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class TransactionRecord {

    public enum TransactionType {
        SALE,       // Seller sold an item
        PURCHASE,   // Buyer purchased an item
        AUCTION_WIN,// Won an auction
        AUCTION_SOLD// Sold via auction
    }

    private UUID id;
    private TransactionType transactionType;
    private ListingType listingType;
    private boolean isPokemon;
    private String itemName;
    private BigDecimal price;
    private String currency;
    private BigDecimal taxDeducted;
    private String otherPartyName;
    private UUID otherPartyUuid;
    private long timestamp;

    /**
     * Create a sale record (for the seller)
     */
    public static TransactionRecord sale(String itemName, BigDecimal price, String currency,
                                          BigDecimal tax, String buyerName, UUID buyerUuid,
                                          boolean isPokemon, ListingType listingType) {
        TransactionRecord record = new TransactionRecord();
        record.id = UUID.randomUUID();
        record.transactionType = listingType == ListingType.AUCTION ?
                TransactionType.AUCTION_SOLD : TransactionType.SALE;
        record.listingType = listingType;
        record.isPokemon = isPokemon;
        record.itemName = itemName;
        record.price = price;
        record.currency = currency;
        record.taxDeducted = tax;
        record.otherPartyName = buyerName;
        record.otherPartyUuid = buyerUuid;
        record.timestamp = System.currentTimeMillis();
        return record;
    }

    /**
     * Create a purchase record (for the buyer)
     */
    public static TransactionRecord purchase(String itemName, BigDecimal price, String currency,
                                              String sellerName, UUID sellerUuid,
                                              boolean isPokemon, ListingType listingType) {
        TransactionRecord record = new TransactionRecord();
        record.id = UUID.randomUUID();
        record.transactionType = listingType == ListingType.AUCTION ?
                TransactionType.AUCTION_WIN : TransactionType.PURCHASE;
        record.listingType = listingType;
        record.isPokemon = isPokemon;
        record.itemName = itemName;
        record.price = price;
        record.currency = currency;
        record.taxDeducted = BigDecimal.ZERO;
        record.otherPartyName = sellerName;
        record.otherPartyUuid = sellerUuid;
        record.timestamp = System.currentTimeMillis();
        return record;
    }
}
