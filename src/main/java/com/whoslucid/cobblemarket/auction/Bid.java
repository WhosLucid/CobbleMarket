package com.whoslucid.cobblemarket.auction;

import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@EqualsAndHashCode
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class Bid {
    private UUID bidderUuid;
    private String bidderName;
    private BigDecimal amount;
    private long timestamp;

    /**
     * Create a new bid
     */
    public static Bid create(UUID bidderUuid, String bidderName, BigDecimal amount) {
        return new Bid(bidderUuid, bidderName, amount, System.currentTimeMillis());
    }
}
