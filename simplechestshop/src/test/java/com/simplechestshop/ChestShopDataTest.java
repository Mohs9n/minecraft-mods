package com.simplechestshop;

import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.server.Bootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class ChestShopDataTest {

    @BeforeAll
    public static void setup() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    public void testShopRegistrationAndOwnership() {
        BlockPos pos = new BlockPos(100, 64, 200);
        UUID ownerId = UUID.randomUUID();
        String ownerName = "TestPlayer";

        // Register shop
        ChestShopData.register(null, pos, ownerId, ownerName);

        // Verify record exists
        ChestShopData.ShopRecord record = ChestShopData.getRecord(pos);
        assertNotNull(record);
        assertEquals(ownerId.toString(), record.ownerUuid);
        assertEquals(ownerName, record.ownerName);
        assertTrue(ChestShopData.isOwner(pos, ownerId));

        // Verify non-owner check
        UUID strangerId = UUID.randomUUID();
        assertFalse(ChestShopData.isOwner(pos, strangerId));

        // Verify remove
        ChestShopData.remove(null, pos);
        assertNull(ChestShopData.getRecord(pos));
        assertFalse(ChestShopData.isOwner(pos, ownerId));
    }

    @Test
    public void testIsProtectedShopWithNullLevel() {
        BlockPos pos = new BlockPos(50, 70, 50);
        assertFalse(ChestShopManager.isProtectedShop(null, pos));
        assertFalse(ChestShopManager.isProtectedShop(null, null));
    }
}
