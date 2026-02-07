package com.whoslucid.cobblemarket.moderation;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.whoslucid.cobblelib.CobbleLib;
import com.whoslucid.cobblelib.util.Utils;
import com.whoslucid.cobblemarket.CobbleMarket;
import com.whoslucid.cobblemarket.util.TimeUtils;
import lombok.Getter;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.lang.reflect.Type;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

@Getter
public class TimeoutManager {

    // Map of player UUID to timeout end timestamp
    private final Map<UUID, Long> timeouts = new ConcurrentHashMap<>();
    private final Gson gson;

    public TimeoutManager() {
        this.gson = new GsonBuilder()
                .setPrettyPrinting()
                .disableHtmlEscaping()
                .create();
    }

    /**
     * Check if a player is timed out
     */
    public boolean isTimedOut(UUID playerUuid) {
        Long endTime = timeouts.get(playerUuid);
        if (endTime == null) return false;

        if (System.currentTimeMillis() > endTime) {
            timeouts.remove(playerUuid);
            save();
            return false;
        }
        return true;
    }

    /**
     * Get remaining timeout time in milliseconds
     */
    public long getRemainingTimeout(UUID playerUuid) {
        Long endTime = timeouts.get(playerUuid);
        if (endTime == null) return 0;
        return Math.max(0, endTime - System.currentTimeMillis());
    }

    /**
     * Get formatted remaining timeout time
     */
    public String getFormattedRemainingTimeout(UUID playerUuid) {
        return TimeUtils.formatDuration(getRemainingTimeout(playerUuid));
    }

    /**
     * Add a timeout to a player
     */
    public void addTimeout(UUID playerUuid, long durationMillis) {
        timeouts.put(playerUuid, System.currentTimeMillis() + durationMillis);
        save();
    }

    /**
     * Remove a player's timeout
     */
    public void removeTimeout(UUID playerUuid) {
        timeouts.remove(playerUuid);
        save();
    }

    /**
     * Clean up expired timeouts
     */
    public void cleanupExpired() {
        long now = System.currentTimeMillis();
        timeouts.entrySet().removeIf(entry -> now > entry.getValue());
    }

    /**
     * Load timeouts from file
     */
    public void load() {
        File file = new File(Utils.getAbsolutePath(CobbleMarket.PATH), "timeouts.json");
        if (!file.exists()) return;

        try (FileReader reader = new FileReader(file)) {
            Type type = new TypeToken<Map<UUID, Long>>() {}.getType();
            Map<UUID, Long> loaded = gson.fromJson(reader, type);
            if (loaded != null) {
                timeouts.clear();
                timeouts.putAll(loaded);
                cleanupExpired();
            }
        } catch (Exception e) {
            CobbleLib.LOGGER.error("Failed to load timeouts: " + e.getMessage());
        }
    }

    /**
     * Save timeouts to file
     */
    public void save() {
        CompletableFuture.runAsync(() -> {
            try {
                File dir = Utils.getAbsolutePath(CobbleMarket.PATH);
                if (!dir.exists()) dir.mkdirs();

                cleanupExpired();

                File file = new File(dir, "timeouts.json");
                try (FileWriter writer = new FileWriter(file)) {
                    gson.toJson(timeouts, writer);
                }
            } catch (Exception e) {
                CobbleLib.LOGGER.error("Failed to save timeouts: " + e.getMessage());
            }
        }, CobbleMarket.EXECUTOR);
    }
}
