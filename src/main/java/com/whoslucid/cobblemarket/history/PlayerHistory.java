package com.whoslucid.cobblemarket.history;

import lombok.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@EqualsAndHashCode
@ToString
public class PlayerHistory {
    private UUID playerUuid;
    private List<TransactionRecord> transactions = new ArrayList<>();

    public PlayerHistory() {
        this.transactions = new ArrayList<>();
    }

    public PlayerHistory(UUID playerUuid) {
        this.playerUuid = playerUuid;
        this.transactions = new ArrayList<>();
    }

    /**
     * Add a transaction record
     */
    public void addTransaction(TransactionRecord record) {
        if (transactions == null) {
            transactions = new ArrayList<>();
        }
        transactions.add(0, record); // Add to front (newest first)

        // Keep only last 100 transactions
        if (transactions.size() > 100) {
            transactions = new ArrayList<>(transactions.subList(0, 100));
        }
    }

    /**
     * Get transactions sorted by timestamp (newest first)
     */
    public List<TransactionRecord> getTransactionsSorted() {
        if (transactions == null) return new ArrayList<>();
        return transactions.stream()
                .sorted((a, b) -> Long.compare(b.getTimestamp(), a.getTimestamp()))
                .toList();
    }

    /**
     * Get total number of transactions
     */
    public int getTransactionCount() {
        return transactions != null ? transactions.size() : 0;
    }
}
