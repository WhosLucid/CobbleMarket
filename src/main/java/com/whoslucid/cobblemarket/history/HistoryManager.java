package com.whoslucid.cobblemarket.history;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.whoslucid.cobblelib.CobbleLib;
import com.whoslucid.cobblelib.util.Utils;
import com.whoslucid.cobblemarket.CobbleMarket;
import lombok.Getter;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

@Getter
public class HistoryManager {

    private final Map<UUID, PlayerHistory> historyCache = new ConcurrentHashMap<>();
    private final Gson gson;

    public HistoryManager() {
        this.gson = new GsonBuilder()
                .setPrettyPrinting()
                .disableHtmlEscaping()
                .create();
    }

    /**
     * Add a transaction to a player's history
     */
    public void addTransaction(UUID playerUuid, TransactionRecord record) {
        PlayerHistory history = getOrCreateHistory(playerUuid);
        history.addTransaction(record);
        saveHistory(playerUuid, history);
    }

    /**
     * Get or create player history
     */
    public PlayerHistory getOrCreateHistory(UUID playerUuid) {
        return historyCache.computeIfAbsent(playerUuid, this::loadHistory);
    }

    /**
     * Get player history (null if doesn't exist)
     */
    public PlayerHistory getHistory(UUID playerUuid) {
        if (historyCache.containsKey(playerUuid)) {
            return historyCache.get(playerUuid);
        }
        PlayerHistory history = loadHistory(playerUuid);
        if (history != null) {
            historyCache.put(playerUuid, history);
        }
        return history;
    }

    /**
     * Load player history from file
     */
    private PlayerHistory loadHistory(UUID playerUuid) {
        File file = new File(Utils.getAbsolutePath(CobbleMarket.PATH_HISTORY),
                playerUuid.toString() + ".json");

        if (!file.exists()) {
            return new PlayerHistory(playerUuid);
        }

        try (FileReader reader = new FileReader(file)) {
            PlayerHistory history = gson.fromJson(reader, PlayerHistory.class);
            if (history == null) {
                history = new PlayerHistory(playerUuid);
            }
            history.setPlayerUuid(playerUuid);
            return history;
        } catch (Exception e) {
            CobbleLib.LOGGER.error("Failed to load history for: " + playerUuid + " - " + e.getMessage());
            return new PlayerHistory(playerUuid);
        }
    }

    /**
     * Save player history to file
     */
    private void saveHistory(UUID playerUuid, PlayerHistory history) {
        CompletableFuture.runAsync(() -> {
            try {
                File dir = Utils.getAbsolutePath(CobbleMarket.PATH_HISTORY);
                if (!dir.exists()) dir.mkdirs();

                File file = new File(dir, playerUuid.toString() + ".json");
                try (FileWriter writer = new FileWriter(file)) {
                    gson.toJson(history, writer);
                }
            } catch (Exception e) {
                CobbleLib.LOGGER.error("Failed to save history for: " + playerUuid + " - " + e.getMessage());
            }
        }, CobbleMarket.EXECUTOR);
    }

    /**
     * Clear cache for a player
     */
    public void clearCache(UUID playerUuid) {
        historyCache.remove(playerUuid);
    }

    /**
     * Save all cached histories
     */
    public void saveAll() {
        for (Map.Entry<UUID, PlayerHistory> entry : historyCache.entrySet()) {
            saveHistory(entry.getKey(), entry.getValue());
        }
    }
}
