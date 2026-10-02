package com.simplechestshop;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class ShopTrade {
    private final Item priceItem;
    private final int priceCount;
    private final Item saleItem;
    private final int saleCount;
    private final String rawText;
    private final boolean valid;

    public ShopTrade(Item priceItem, int priceCount, Item saleItem, int saleCount, String rawText) {
        this.priceItem = priceItem;
        this.priceCount = priceCount;
        this.saleItem = saleItem;
        this.saleCount = saleCount;
        this.rawText = rawText;
        this.valid = (priceItem != null && priceCount > 0 && (saleItem == null || saleCount > 0));
    }

    public static ShopTrade invalid(String rawText) {
        return new ShopTrade(null, 0, null, 0, rawText);
    }

    public boolean isValid() {
        return valid;
    }

    public Item getPriceItem() {
        return priceItem;
    }

    public int getPriceCount() {
        return priceCount;
    }

    public Item getSaleItem() {
        return saleItem;
    }

    public int getSaleCount() {
        return saleCount;
    }

    public String getRawText() {
        return rawText;
    }

    public boolean matchesPayment(ItemStack held) {
        if (!valid || held.isEmpty()) {
            return false;
        }
        return held.getItem() == priceItem && held.getCount() >= priceCount;
    }
}
