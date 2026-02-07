package com.whoslucid.cobblemarket.integration;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.whoslucid.cobblelib.CobbleLib;
import com.whoslucid.cobblemarket.CobbleMarket;
import com.whoslucid.cobblemarket.auction.Auction;
import com.whoslucid.cobblemarket.listing.Listing;
import com.whoslucid.cobblemarket.listing.PokemonListing;
import com.whoslucid.cobblemarket.util.MarketUtils;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

public class DiscordWebhook {

    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    /**
     * Send notification for new listing
     */
    public static void sendNewListingNotification(Listing<?> listing) {
        if (!isEnabled() || !CobbleMarket.config.getDiscord().isNotifyNewListings()) {
            return;
        }

        JsonObject embed = new JsonObject();
        embed.addProperty("title", "New Listing");
        embed.addProperty("color", 5763719); // Green

        StringBuilder description = new StringBuilder();
        description.append("**").append(listing.getDisplayName()).append("**\n\n");
        description.append("**Seller:** ").append(listing.getSellerName()).append("\n");
        description.append("**Price:** ").append(MarketUtils.formatPrice(listing.getPrice()))
                .append(" ").append(listing.getCurrency().getCurrency()).append("\n");
        description.append("**Duration:** ").append(listing.getFormattedRemainingTime()).append("\n");

        if (listing instanceof PokemonListing pokemon) {
            description.append("\n**Level:** ").append(pokemon.getLevel()).append("\n");
            description.append("**Shiny:** ").append(pokemon.isShiny() ? "Yes" : "No").append("\n");
            description.append("**Perfect IVs:** ").append(pokemon.getPerfectIvCount()).append("\n");
            if (pokemon.isLegendary()) {
                description.append("**Rarity:** Legendary\n");
            }
        }

        if (listing.isAuction()) {
            embed.addProperty("title", "New Auction Started");
            embed.addProperty("color", 16776960); // Gold
        }

        embed.addProperty("description", description.toString());
        embed.addProperty("timestamp", java.time.Instant.now().toString());

        JsonObject footer = new JsonObject();
        footer.addProperty("text", "CobbleMarket");
        embed.add("footer", footer);

        sendEmbed(embed);
    }

    /**
     * Send notification for sale
     */
    public static void sendSaleNotification(Listing<?> listing, String buyerName) {
        if (!isEnabled() || !CobbleMarket.config.getDiscord().isNotifySales()) {
            return;
        }

        JsonObject embed = new JsonObject();
        embed.addProperty("title", "Item Sold!");
        embed.addProperty("color", 3066993); // Blue

        StringBuilder description = new StringBuilder();
        description.append("**").append(listing.getDisplayName()).append("**\n\n");
        description.append("**Seller:** ").append(listing.getSellerName()).append("\n");
        description.append("**Buyer:** ").append(buyerName).append("\n");
        description.append("**Price:** ").append(MarketUtils.formatPrice(listing.getPrice()))
                .append(" ").append(listing.getCurrency().getCurrency()).append("\n");

        embed.addProperty("description", description.toString());
        embed.addProperty("timestamp", java.time.Instant.now().toString());

        JsonObject footer = new JsonObject();
        footer.addProperty("text", "CobbleMarket");
        embed.add("footer", footer);

        sendEmbed(embed);
    }

    /**
     * Send notification for auction end
     */
    public static void sendAuctionEndNotification(Auction auction) {
        if (!isEnabled() || !CobbleMarket.config.getDiscord().isNotifyAuctionEnd()) {
            return;
        }

        JsonObject embed = new JsonObject();

        if (auction.hasBids()) {
            embed.addProperty("title", "Auction Ended - Sold!");
            embed.addProperty("color", 15844367); // Gold

            StringBuilder description = new StringBuilder();
            description.append("**").append(auction.getDisplayName()).append("**\n\n");
            description.append("**Seller:** ").append(auction.getSellerName()).append("\n");
            description.append("**Winner:** ").append(auction.getHighestBidderName()).append("\n");
            description.append("**Final Price:** ").append(MarketUtils.formatPrice(auction.getCurrentBid()))
                    .append(" ").append(auction.getCurrency().getCurrency()).append("\n");
            description.append("**Total Bids:** ").append(auction.getBidCount()).append("\n");

            embed.addProperty("description", description.toString());
        } else {
            embed.addProperty("title", "Auction Ended - No Bids");
            embed.addProperty("color", 15158332); // Red

            StringBuilder description = new StringBuilder();
            description.append("**").append(auction.getDisplayName()).append("**\n\n");
            description.append("**Seller:** ").append(auction.getSellerName()).append("\n");
            description.append("**Starting Price:** ").append(MarketUtils.formatPrice(auction.getStartingPrice()))
                    .append(" ").append(auction.getCurrency().getCurrency()).append("\n");
            description.append("\nThe auction ended with no bids.");

            embed.addProperty("description", description.toString());
        }

        embed.addProperty("timestamp", java.time.Instant.now().toString());

        JsonObject footer = new JsonObject();
        footer.addProperty("text", "CobbleMarket");
        embed.add("footer", footer);

        sendEmbed(embed);
    }

    /**
     * Send an embed to the Discord webhook
     */
    private static void sendEmbed(JsonObject embed) {
        CompletableFuture.runAsync(() -> {
            try {
                String webhookUrl = CobbleMarket.config.getDiscord().getWebhookUrl();
                if (webhookUrl == null || webhookUrl.isEmpty()) {
                    return;
                }

                JsonObject payload = new JsonObject();
                payload.addProperty("username", "CobbleMarket");

                JsonArray embeds = new JsonArray();
                embeds.add(embed);
                payload.add("embeds", embeds);

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(webhookUrl))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(payload.toString()))
                        .timeout(Duration.ofSeconds(10))
                        .build();

                HTTP_CLIENT.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                        .exceptionally(e -> {
                            CobbleLib.LOGGER.error("Failed to send Discord webhook: " + e.getMessage());
                            return null;
                        });

            } catch (Exception e) {
                CobbleLib.LOGGER.error("Error sending Discord webhook: " + e.getMessage());
            }
        }, CobbleMarket.EXECUTOR);
    }

    /**
     * Check if Discord integration is enabled
     */
    private static boolean isEnabled() {
        return CobbleMarket.config.getDiscord().isEnabled() &&
                CobbleMarket.config.getDiscord().getWebhookUrl() != null &&
                !CobbleMarket.config.getDiscord().getWebhookUrl().isEmpty();
    }
}
