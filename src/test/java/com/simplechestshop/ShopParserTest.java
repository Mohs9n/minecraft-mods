package com.simplechestshop;

import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ShopParserTest {

    @BeforeAll
    public static void setup() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    public void testArrowFormat() {
        ShopTrade trade = ShopParser.parseTrade("1 diamond -> 16 oak_log");
        assertTrue(trade.isValid(), "Trade should be valid");
        assertEquals(Items.DIAMOND, trade.getPriceItem());
        assertEquals(1, trade.getPriceCount());
        assertEquals(Items.OAK_LOG, trade.getSaleItem());
        assertEquals(16, trade.getSaleCount());
    }

    @Test
    public void testEqualsFormatWithPrefix() {
        ShopTrade trade = ShopParser.parseTrade("[Shop] 2 emeralds = 64 cooked_beef");
        assertTrue(trade.isValid(), "Trade should be valid");
        assertEquals(Items.EMERALD, trade.getPriceItem());
        assertEquals(2, trade.getPriceCount());
        assertEquals(Items.COOKED_BEEF, trade.getSaleItem());
        assertEquals(64, trade.getSaleCount());
    }

    @Test
    public void testForFormat() {
        ShopTrade trade = ShopParser.parseTrade("32 iron_ingot for 1 diamond");
        assertTrue(trade.isValid(), "Trade should be valid");
        assertEquals(Items.DIAMOND, trade.getPriceItem());
        assertEquals(1, trade.getPriceCount());
        assertEquals(Items.IRON_INGOT, trade.getSaleItem());
        assertEquals(32, trade.getSaleCount());
    }

    @Test
    public void testSimplePriceOnly() {
        ShopTrade trade = ShopParser.parseTrade("1 diamond");
        assertTrue(trade.isValid(), "Trade should be valid");
        assertEquals(Items.DIAMOND, trade.getPriceItem());
        assertEquals(1, trade.getPriceCount());
        assertNull(trade.getSaleItem(), "Sale item should be dynamic when omitted");
        assertEquals(1, trade.getSaleCount());
    }

    @Test
    public void testSpacesInItemNames() {
        ShopTrade trade = ShopParser.parseTrade("1 diamond -> 16 oak log");
        assertTrue(trade.isValid(), "Trade should be valid with spaces in item names");
        assertEquals(Items.OAK_LOG, trade.getSaleItem());
    }

    @Test
    public void testPluralNormalization() {
        ShopTrade trade = ShopParser.parseTrade("5 diamonds -> 10 apples");
        assertTrue(trade.isValid(), "Trade should be valid with plurals");
        assertEquals(Items.DIAMOND, trade.getPriceItem());
        assertEquals(5, trade.getPriceCount());
        assertEquals(Items.APPLE, trade.getSaleItem());
        assertEquals(10, trade.getSaleCount());
    }

    @Test
    public void testInvalidItemReturnsInvalidTrade() {
        ShopTrade trade = ShopParser.parseTrade("1 imaginary_item_xyz -> 16 oak_log");
        assertFalse(trade.isValid(), "Trade should be invalid if item doesn't exist");
    }

    @Test
    public void testMultiPriceOrFormat() {
        java.util.List<ShopTrade> trades = ShopParser.parseTrades("1 diamond or 10 iron_ingot -> 64 cooked_beef");
        assertEquals(2, trades.size(), "Should parse 2 alternative trades");

        ShopTrade trade1 = trades.get(0);
        assertTrue(trade1.isValid());
        assertEquals(Items.DIAMOND, trade1.getPriceItem());
        assertEquals(1, trade1.getPriceCount());
        assertEquals(Items.COOKED_BEEF, trade1.getSaleItem());
        assertEquals(64, trade1.getSaleCount());

        ShopTrade trade2 = trades.get(1);
        assertTrue(trade2.isValid());
        assertEquals(Items.IRON_INGOT, trade2.getPriceItem());
        assertEquals(10, trade2.getPriceCount());
        assertEquals(Items.COOKED_BEEF, trade2.getSaleItem());
        assertEquals(64, trade2.getSaleCount());
    }

    @Test
    public void testMultiPriceSlashAndPipeFormat() {
        java.util.List<ShopTrade> slashTrades = ShopParser.parseTrades("1 diamond / 10 iron_ingot -> 64 cooked_beef");
        assertEquals(2, slashTrades.size());
        assertEquals(Items.DIAMOND, slashTrades.get(0).getPriceItem());
        assertEquals(Items.IRON_INGOT, slashTrades.get(1).getPriceItem());

        java.util.List<ShopTrade> pipeTrades = ShopParser.parseTrades("1 diamond | 10 iron_ingot | 32 coal -> 64 cooked_beef");
        assertEquals(3, pipeTrades.size());
        assertEquals(Items.DIAMOND, pipeTrades.get(0).getPriceItem());
        assertEquals(Items.IRON_INGOT, pipeTrades.get(1).getPriceItem());
        assertEquals(Items.COAL, pipeTrades.get(2).getPriceItem());
        assertEquals(32, pipeTrades.get(2).getPriceCount());
        assertEquals(Items.COOKED_BEEF, pipeTrades.get(2).getSaleItem());
        assertEquals(64, pipeTrades.get(2).getSaleCount());
    }

    @Test
    public void testMultiPriceForFormat() {
        java.util.List<ShopTrade> trades = ShopParser.parseTrades("64 cooked_beef for 1 diamond or 10 iron_ingot");
        assertEquals(2, trades.size());
        assertEquals(Items.DIAMOND, trades.get(0).getPriceItem());
        assertEquals(1, trades.get(0).getPriceCount());
        assertEquals(Items.IRON_INGOT, trades.get(1).getPriceItem());
        assertEquals(10, trades.get(1).getPriceCount());
        assertEquals(Items.COOKED_BEEF, trades.get(0).getSaleItem());
        assertEquals(64, trades.get(0).getSaleCount());
        assertEquals(Items.COOKED_BEEF, trades.get(1).getSaleItem());
        assertEquals(64, trades.get(1).getSaleCount());
    }
}
