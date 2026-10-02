package com.simplechestshop;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ShopParser {

    private static final Pattern ARROW_PATTERN = Pattern.compile("^(.+?)\\s*(?:->|=>|=|:)\\s*(.+)$");
    private static final Pattern FOR_PATTERN = Pattern.compile("^(.+?)\\s+for\\s+(.+)$", Pattern.CASE_INSENSITIVE);
    private static final Pattern COUNT_ITEM_PATTERN = Pattern.compile("^(\\d+)\\s*(?:x\\s*)?(.+)$", Pattern.CASE_INSENSITIVE);

    /**
     * Checks if an ItemStack is a valid shop paper/config item.
     */
    public static boolean isShopConfigItem(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        // Only paper or name tag can configure a shop
        if (stack.getItem() != Items.PAPER && stack.getItem() != Items.NAME_TAG) {
            return false;
        }
        return stack.has(net.minecraft.core.component.DataComponents.CUSTOM_NAME);
    }

    /**
     * Parses the custom name of the paper into one or more ShopTrades.
     * Supports multiple alternative prices using "or", "/", or "|".
     * E.g. "1 diamond or 10 iron_ingot -> 64 cooked_beef"
     */
    public static java.util.List<ShopTrade> parseTrades(String customName) {
        java.util.List<ShopTrade> list = new java.util.ArrayList<>();
        if (customName == null || customName.isBlank()) {
            return list;
        }

        String cleaned = customName.trim();
        // Remove common prefixes
        if (cleaned.toLowerCase(Locale.ROOT).startsWith("[shop]")) {
            cleaned = cleaned.substring(6).trim();
        } else if (cleaned.toLowerCase(Locale.ROOT).startsWith("shop:")) {
            cleaned = cleaned.substring(5).trim();
        } else if (cleaned.toLowerCase(Locale.ROOT).startsWith("price:")) {
            cleaned = cleaned.substring(6).trim();
        }

        // Try Pattern 1: Arrow/Equals/Colon: "1 diamond or 10 iron -> 64 cooked_beef" (Price -> Sale)
        Matcher arrowMatcher = ARROW_PATTERN.matcher(cleaned);
        if (arrowMatcher.matches()) {
            String pricePart = arrowMatcher.group(1).trim();
            String salePart = arrowMatcher.group(2).trim();

            ParsedItem sale = parseCountAndItem(salePart);
            if (sale != null) {
                String[] priceTokens = splitAlternativePrices(pricePart);
                for (String token : priceTokens) {
                    ParsedItem price = parseCountAndItem(token);
                    if (price != null) {
                        list.add(new ShopTrade(price.item, price.count, sale.item, sale.count, customName));
                    }
                }
                if (!list.isEmpty()) {
                    return list;
                }
            }
        }

        // Try Pattern 2: "64 cooked_beef for 1 diamond or 10 iron" (Sale for Price)
        Matcher forMatcher = FOR_PATTERN.matcher(cleaned);
        if (forMatcher.matches()) {
            String salePart = forMatcher.group(1).trim();
            String pricePart = forMatcher.group(2).trim();

            ParsedItem sale = parseCountAndItem(salePart);
            if (sale != null) {
                String[] priceTokens = splitAlternativePrices(pricePart);
                for (String token : priceTokens) {
                    ParsedItem price = parseCountAndItem(token);
                    if (price != null) {
                        list.add(new ShopTrade(price.item, price.count, sale.item, sale.count, customName));
                    }
                }
                if (!list.isEmpty()) {
                    return list;
                }
            }
        }

        // Try Pattern 3: Simple Price only: "1 diamond or 2 emeralds"
        String[] priceTokens = splitAlternativePrices(cleaned);
        for (String token : priceTokens) {
            ParsedItem price = parseCountAndItem(token);
            if (price != null) {
                list.add(new ShopTrade(price.item, price.count, null, 1, customName));
            }
        }

        return list;
    }

    /**
     * Parses a single trade, returning the primary trade or invalid.
     */
    public static ShopTrade parseTrade(String customName) {
        java.util.List<ShopTrade> list = parseTrades(customName);
        return list.isEmpty() ? ShopTrade.invalid(customName) : list.get(0);
    }

    private static String[] splitAlternativePrices(String text) {
        if (text == null) return new String[0];
        return text.split("\\s+(?:or|\\/|\\|)\\s*|\\s*\\/\\s*|\\s*\\|\\s*");
    }

    private static ParsedItem parseCountAndItem(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }

        text = text.trim();
        int count = 1;
        String itemName = text;

        Matcher m = COUNT_ITEM_PATTERN.matcher(text);
        if (m.matches()) {
            try {
                count = Integer.parseInt(m.group(1));
            } catch (NumberFormatException ignored) {
                count = 1;
            }
            itemName = m.group(2).trim();
        }

        if (count <= 0) {
            count = 1;
        }

        Item item = resolveItem(itemName);
        if (item == null || item == Items.AIR) {
            return null;
        }

        return new ParsedItem(item, count);
    }

    public static Item resolveItem(String name) {
        if (name == null || name.isBlank()) {
            return null;
        }

        String raw = name.trim().toLowerCase(Locale.ROOT).replace(' ', '_');

        // Check direct resource location
        Identifier id = Identifier.tryParse(raw.contains(":") ? raw : "minecraft:" + raw);
        if (id != null) {
            Optional<Item> direct = BuiltInRegistries.ITEM.getOptional(id);
            if (direct.isPresent() && direct.get() != Items.AIR) {
                return direct.get();
            }
        }

        // Try removing plural 's' or 'es' (e.g. diamonds -> diamond, apples -> apple)
        if (raw.endsWith("es")) {
            String singular = raw.substring(0, raw.length() - 2);
            Identifier singId = Identifier.tryParse("minecraft:" + singular);
            if (singId != null) {
                Optional<Item> item = BuiltInRegistries.ITEM.getOptional(singId);
                if (item.isPresent() && item.get() != Items.AIR) {
                    return item.get();
                }
            }
        }
        if (raw.endsWith("s")) {
            String singular = raw.substring(0, raw.length() - 1);
            Identifier singId = Identifier.tryParse("minecraft:" + singular);
            if (singId != null) {
                Optional<Item> item = BuiltInRegistries.ITEM.getOptional(singId);
                if (item.isPresent() && item.get() != Items.AIR) {
                    return item.get();
                }
            }
        }

        // Try searching registry paths
        for (Item item : BuiltInRegistries.ITEM) {
            Identifier key = BuiltInRegistries.ITEM.getKey(item);
            if (key.getPath().equalsIgnoreCase(raw)) {
                return item;
            }
        }

        return null;
    }

    private static class ParsedItem {
        final Item item;
        final int count;

        ParsedItem(Item item, int count) {
            this.item = item;
            this.count = count;
        }
    }
}
