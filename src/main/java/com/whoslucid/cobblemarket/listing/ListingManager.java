package com.whoslucid.cobblemarket.listing;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.whoslucid.cobblelib.CobbleLib;
import com.whoslucid.cobblelib.util.Utils;
import com.whoslucid.cobblemarket.CobbleMarket;
import com.whoslucid.cobblemarket.auction.Auction;
import lombok.Getter;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Getter
public class ListingManager {

    private final List<Listing<?>> activeListings = new CopyOnWriteArrayList<>();
    private final Map<UUID, List<Listing<?>>> expiredListings = new ConcurrentHashMap<>();
    private final Gson gson;

    public ListingManager() {
        this.gson = new GsonBuilder()
                .setPrettyPrinting()
                .disableHtmlEscaping()
                .create();
    }

    // ==================== CRUD Operations ====================

    /**
     * Add a new listing
     */
    public void addListing(Listing<?> listing) {
        if (listing == null || !listing.isValid()) return;

        activeListings.add(listing);
        saveListing(listing);

        if (CobbleMarket.config.isDebug()) {
            CobbleLib.LOGGER.info("Added listing: " + listing.getId());
        }
    }

    /**
     * Remove a listing by ID
     */
    public boolean removeListing(UUID listingId) {
        Listing<?> listing = getListing(listingId);
        if (listing == null) return false;

        activeListings.remove(listing);
        deleteListingFile(listingId);

        if (CobbleMarket.config.isDebug()) {
            CobbleLib.LOGGER.info("Removed listing: " + listingId);
        }
        return true;
    }

    /**
     * Get a listing by ID
     */
    public Listing<?> getListing(UUID listingId) {
        return activeListings.stream()
                .filter(l -> l.getId().equals(listingId))
                .findFirst()
                .orElse(null);
    }

    /**
     * Move a listing to expired
     */
    public void expireListing(Listing<?> listing) {
        if (listing == null) return;

        activeListings.remove(listing);
        deleteListingFile(listing.getId());

        expiredListings.computeIfAbsent(listing.getSellerUuid(), k -> new CopyOnWriteArrayList<>())
                .add(listing);
        saveExpiredListing(listing);

        if (CobbleMarket.config.isDebug()) {
            CobbleLib.LOGGER.info("Expired listing: " + listing.getId());
        }
    }

    /**
     * Relist an expired listing
     */
    public void relist(Listing<?> listing, long newDurationMillis) {
        if (listing == null) return;

        // Remove from expired
        List<Listing<?>> playerExpired = expiredListings.get(listing.getSellerUuid());
        if (playerExpired != null) {
            playerExpired.remove(listing);
            deleteExpiredListingFile(listing.getSellerUuid(), listing.getId());
        }

        // Reset duration and add back to active
        listing.resetDuration(newDurationMillis);
        listing.setCreatedTime(System.currentTimeMillis());
        addListing(listing);
    }

    /**
     * Reclaim an expired listing (remove from expired, return item to player)
     */
    public Listing<?> reclaimExpired(UUID playerUuid, UUID listingId) {
        List<Listing<?>> playerExpired = expiredListings.get(playerUuid);
        if (playerExpired == null) return null;

        Listing<?> listing = playerExpired.stream()
                .filter(l -> l.getId().equals(listingId))
                .findFirst()
                .orElse(null);

        if (listing != null) {
            playerExpired.remove(listing);
            deleteExpiredListingFile(playerUuid, listingId);
        }
        return listing;
    }

    // ==================== Query Methods ====================

    /**
     * Get all active listings
     */
    public List<Listing<?>> getAllListings() {
        return new ArrayList<>(activeListings);
    }

    /**
     * Get all Pokemon listings
     */
    public List<PokemonListing> getPokemonListings() {
        return activeListings.stream()
                .filter(l -> l instanceof PokemonListing && !l.isAuction())
                .map(l -> (PokemonListing) l)
                .collect(Collectors.toList());
    }

    /**
     * Get all Item listings
     */
    public List<ItemListing> getItemListings() {
        return activeListings.stream()
                .filter(l -> l instanceof ItemListing)
                .map(l -> (ItemListing) l)
                .collect(Collectors.toList());
    }

    /**
     * Get all active auctions
     */
    public List<Auction> getAuctions() {
        return activeListings.stream()
                .filter(l -> l instanceof Auction)
                .map(l -> (Auction) l)
                .collect(Collectors.toList());
    }

    /**
     * Get shiny Pokemon listings
     */
    public List<PokemonListing> getShinyListings() {
        return activeListings.stream()
                .filter(l -> l instanceof PokemonListing pl && pl.isShiny())
                .map(l -> (PokemonListing) l)
                .collect(Collectors.toList());
    }

    /**
     * Get legendary Pokemon listings
     */
    public List<PokemonListing> getLegendaryListings() {
        return activeListings.stream()
                .filter(l -> l instanceof PokemonListing pl && pl.isLegendary())
                .map(l -> (PokemonListing) l)
                .collect(Collectors.toList());
    }

    /**
     * Search listings by query
     */
    public List<Listing<?>> searchListings(String query) {
        if (query == null || query.isEmpty()) {
            return getAllListings();
        }
        String lowerQuery = query.toLowerCase();
        return activeListings.stream()
                .filter(l -> l.getSearchableText().contains(lowerQuery))
                .collect(Collectors.toList());
    }

    /**
     * Get listings with a filter predicate
     */
    public List<Listing<?>> getFilteredListings(Predicate<Listing<?>> filter) {
        return activeListings.stream()
                .filter(filter)
                .collect(Collectors.toList());
    }

    /**
     * Get a player's active listings
     */
    public List<Listing<?>> getPlayerListings(UUID playerUuid) {
        return activeListings.stream()
                .filter(l -> l.isSeller(playerUuid))
                .collect(Collectors.toList());
    }

    /**
     * Get a player's expired listings
     */
    public List<Listing<?>> getPlayerExpiredListings(UUID playerUuid) {
        return expiredListings.getOrDefault(playerUuid, Collections.emptyList());
    }

    /**
     * Get count of player's active listings
     */
    public int getPlayerListingCount(UUID playerUuid) {
        return (int) activeListings.stream()
                .filter(l -> l.isSeller(playerUuid))
                .count();
    }

    // ==================== Expiration & Auction Checks ====================

    /**
     * Check all listings for expiration
     */
    public void checkExpirations() {
        List<Listing<?>> toExpire = activeListings.stream()
                .filter(Listing::isExpired)
                .filter(l -> !l.isAuction()) // Auctions handled separately
                .collect(Collectors.toList());

        for (Listing<?> listing : toExpire) {
            expireListing(listing);
            // TODO: Notify seller
        }
    }

    /**
     * Check auctions for ending
     */
    public void checkAuctionEndings() {
        List<Auction> endedAuctions = activeListings.stream()
                .filter(l -> l instanceof Auction a && a.isExpired())
                .map(l -> (Auction) l)
                .collect(Collectors.toList());

        for (Auction auction : endedAuctions) {
            processAuctionEnd(auction);
        }
    }

    /**
     * Process an ended auction
     */
    public void processAuctionEnd(Auction auction) {
        activeListings.remove(auction);
        deleteListingFile(auction.getId());

        if (auction.hasBids()) {
            // Auction sold - process via AuctionManager
            com.whoslucid.cobblemarket.auction.AuctionManager.processAuctionEnd(auction);

            if (CobbleMarket.config.isDebug()) {
                CobbleLib.LOGGER.info("Auction ended with winner: " + auction.getHighestBidderName());
            }
        } else {
            // No bids - move to expired
            expiredListings.computeIfAbsent(auction.getSellerUuid(), k -> new CopyOnWriteArrayList<>())
                    .add(auction);
            saveExpiredListing(auction);

            com.whoslucid.cobblemarket.auction.AuctionManager.processAuctionEnd(auction);

            if (CobbleMarket.config.isDebug()) {
                CobbleLib.LOGGER.info("Auction ended with no bids: " + auction.getId());
            }
        }
    }

    // ==================== File I/O ====================

    /**
     * Save a single listing to file
     */
    public void saveListing(Listing<?> listing) {
        CompletableFuture.runAsync(() -> {
            try {
                File dir = Utils.getAbsolutePath(CobbleMarket.PATH_LISTINGS);
                if (!dir.exists()) dir.mkdirs();

                File file = new File(dir, listing.getId().toString() + ".json");
                try (FileWriter writer = new FileWriter(file)) {
                    gson.toJson(listing, writer);
                }
            } catch (Exception e) {
                CobbleLib.LOGGER.error("Failed to save listing: " + listing.getId() + " - " + e.getMessage());
            }
        }, CobbleMarket.EXECUTOR);
    }

    /**
     * Save an expired listing to file
     */
    private void saveExpiredListing(Listing<?> listing) {
        CompletableFuture.runAsync(() -> {
            try {
                File dir = new File(Utils.getAbsolutePath(CobbleMarket.PATH_EXPIRED),
                        listing.getSellerUuid().toString());
                if (!dir.exists()) dir.mkdirs();

                File file = new File(dir, listing.getId().toString() + ".json");
                try (FileWriter writer = new FileWriter(file)) {
                    gson.toJson(listing, writer);
                }
            } catch (Exception e) {
                CobbleLib.LOGGER.error("Failed to save expired listing: " + listing.getId() + " - " + e.getMessage());
            }
        }, CobbleMarket.EXECUTOR);
    }

    /**
     * Delete a listing file
     */
    private void deleteListingFile(UUID listingId) {
        CompletableFuture.runAsync(() -> {
            File file = new File(Utils.getAbsolutePath(CobbleMarket.PATH_LISTINGS),
                    listingId.toString() + ".json");
            if (file.exists()) file.delete();
        }, CobbleMarket.EXECUTOR);
    }

    /**
     * Delete an expired listing file
     */
    private void deleteExpiredListingFile(UUID playerUuid, UUID listingId) {
        CompletableFuture.runAsync(() -> {
            File file = new File(new File(Utils.getAbsolutePath(CobbleMarket.PATH_EXPIRED),
                    playerUuid.toString()), listingId.toString() + ".json");
            if (file.exists()) file.delete();
        }, CobbleMarket.EXECUTOR);
    }

    /**
     * Load all listings from disk
     */
    public void loadAll() {
        activeListings.clear();
        expiredListings.clear();

        // Load active listings
        File listingsDir = Utils.getAbsolutePath(CobbleMarket.PATH_LISTINGS);
        if (listingsDir.exists() && listingsDir.isDirectory()) {
            File[] files = listingsDir.listFiles((dir, name) -> name.endsWith(".json"));
            if (files != null) {
                for (File file : files) {
                    loadListingFile(file);
                }
            }
        }

        // Load expired listings
        File expiredDir = Utils.getAbsolutePath(CobbleMarket.PATH_EXPIRED);
        if (expiredDir.exists() && expiredDir.isDirectory()) {
            File[] playerDirs = expiredDir.listFiles(File::isDirectory);
            if (playerDirs != null) {
                for (File playerDir : playerDirs) {
                    UUID playerUuid = UUID.fromString(playerDir.getName());
                    File[] files = playerDir.listFiles((dir, name) -> name.endsWith(".json"));
                    if (files != null) {
                        for (File file : files) {
                            Listing<?> listing = loadListingFromFile(file);
                            if (listing != null) {
                                expiredListings.computeIfAbsent(playerUuid, k -> new CopyOnWriteArrayList<>())
                                        .add(listing);
                            }
                        }
                    }
                }
            }
        }

        CobbleLib.LOGGER.info("Loaded " + activeListings.size() + " active listings");
    }

    /**
     * Load a listing from file and add to active
     */
    private void loadListingFile(File file) {
        Listing<?> listing = loadListingFromFile(file);
        if (listing != null && listing.isValid()) {
            activeListings.add(listing);
        }
    }

    /**
     * Load a listing from a file
     */
    private Listing<?> loadListingFromFile(File file) {
        try (FileReader reader = new FileReader(file)) {
            JsonElement json = JsonParser.parseReader(reader);
            if (!json.isJsonObject()) return null;

            // Determine type
            boolean isPokemon = json.getAsJsonObject().has("pokemonData");
            boolean isAuction = json.getAsJsonObject().has("bidHistory");

            if (isAuction) {
                return gson.fromJson(json, Auction.class);
            } else if (isPokemon) {
                return gson.fromJson(json, PokemonListing.class);
            } else {
                return gson.fromJson(json, ItemListing.class);
            }
        } catch (Exception e) {
            CobbleLib.LOGGER.error("Failed to load listing from: " + file.getName() + " - " + e.getMessage());
            return null;
        }
    }

    /**
     * Save all listings to disk
     */
    public void saveAll() {
        for (Listing<?> listing : activeListings) {
            saveListing(listing);
        }
        for (Map.Entry<UUID, List<Listing<?>>> entry : expiredListings.entrySet()) {
            for (Listing<?> listing : entry.getValue()) {
                saveExpiredListing(listing);
            }
        }
    }
}
