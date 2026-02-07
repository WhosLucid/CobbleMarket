package com.whoslucid.cobblemarket.listing;

import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import com.whoslucid.cobblelib.Model.EconomyUse;
import com.whoslucid.cobblemarket.CobbleMarket;
import lombok.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class ItemListing extends Listing<ItemStack> {

    // Serialized ItemStack data using Minecraft's CODEC
    private JsonElement itemData;

    // Cached attributes
    private String itemId;
    private String itemName;
    private int count;

    /**
     * Create a new Item listing
     */
    public ItemListing(UUID sellerUuid, String sellerName, BigDecimal price,
                       ItemStack itemStack, EconomyUse currency, long durationMillis) {
        this.id = UUID.randomUUID();
        this.sellerUuid = sellerUuid;
        this.sellerName = sellerName;
        this.price = price;
        this.currency = currency;
        this.createdTime = System.currentTimeMillis();
        this.endTime = durationMillis > 0 ? createdTime + durationMillis : -1;
        this.listingType = ListingType.FIXED_PRICE;
        this.isPokemon = false;

        // Serialize ItemStack
        this.itemData = ItemStack.OPTIONAL_CODEC.encodeStart(JsonOps.INSTANCE, itemStack)
                .resultOrPartial(error -> CobbleMarket.server.execute(() ->
                        com.whoslucid.cobblelib.CobbleLib.LOGGER.error("Error encoding ItemStack: " + error)))
                .orElse(null);

        // Cache attributes
        cacheAttributes(itemStack);
    }

    /**
     * Constructor for deserialization (GSON)
     */
    public ItemListing() {
        this.isPokemon = false;
    }

    /**
     * Cache searchable attributes from the ItemStack
     */
    private void cacheAttributes(ItemStack item) {
        if (item == null || item.isEmpty()) return;

        this.itemId = BuiltInRegistries.ITEM.getKey(item.getItem()).toString();
        this.itemName = item.getHoverName().getString();
        this.count = item.getCount();
    }

    @Override
    public ItemStack getItem() {
        return getItemStack();
    }

    /**
     * Deserialize and return the ItemStack
     */
    public ItemStack getItemStack() {
        if (itemData == null) return ItemStack.EMPTY;

        try {
            return ItemStack.OPTIONAL_CODEC.decode(JsonOps.INSTANCE, itemData)
                    .resultOrPartial(error -> CobbleMarket.server.execute(() ->
                            com.whoslucid.cobblelib.CobbleLib.LOGGER.error("Error decoding ItemStack: " + error)))
                    .map(pair -> pair.getFirst())
                    .orElse(ItemStack.EMPTY);
        } catch (Exception e) {
            com.whoslucid.cobblelib.CobbleLib.LOGGER.error("Failed to deserialize ItemStack: " + e.getMessage());
            return ItemStack.EMPTY;
        }
    }

    @Override
    public ItemStack getDisplayItem() {
        return getItemStack();
    }

    @Override
    public String getDisplayName() {
        ItemStack item = getItemStack();
        if (!item.isEmpty()) {
            return item.getHoverName().getString();
        }
        return itemName != null ? itemName : "Unknown Item";
    }

    @Override
    public String getSearchableText() {
        StringBuilder sb = new StringBuilder();
        sb.append(itemId != null ? itemId.toLowerCase() : "");
        sb.append(" ");
        sb.append(itemName != null ? itemName.toLowerCase() : "");
        sb.append(" ");
        sb.append(sellerName != null ? sellerName.toLowerCase() : "");
        return sb.toString();
    }

    @Override
    public boolean isValid() {
        return itemData != null && !getItemStack().isEmpty();
    }

    /**
     * Refresh cached attributes from current item data
     */
    public void refreshCache() {
        ItemStack item = getItemStack();
        if (!item.isEmpty()) {
            cacheAttributes(item);
        }
    }
}
