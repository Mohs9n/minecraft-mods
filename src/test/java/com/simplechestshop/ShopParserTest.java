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
}
