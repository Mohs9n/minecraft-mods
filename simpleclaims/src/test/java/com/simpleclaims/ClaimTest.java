package com.simpleclaims;

import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class ClaimTest {

    @Test
    public void testCoordinateBoundsAndAllYLevels() {
        UUID owner = UUID.randomUUID();
        // Claim from (10, 20) to (50, 60) in overworld
        Claim claim = new Claim("test-1", "Base", "minecraft:overworld", 10, 20, 50, 60, owner, "Steve");

        assertEquals(10, claim.getMinX());
        assertEquals(50, claim.getMaxX());
        assertEquals(20, claim.getMinZ());
        assertEquals(60, claim.getMaxZ());
        assertEquals(41, claim.getWidthX());
        assertEquals(41, claim.getWidthZ());
        assertEquals(41 * 41, claim.getArea());

        // Points inside (all Y levels: bedrock to sky)
        assertTrue(claim.contains("minecraft:overworld", new BlockPos(10, -64, 20))); // bottom corner
        assertTrue(claim.contains("minecraft:overworld", new BlockPos(50, 319, 60))); // top corner
        assertTrue(claim.contains("minecraft:overworld", new BlockPos(30, 100, 40))); // center

        // Points outside
        assertFalse(claim.contains("minecraft:overworld", new BlockPos(9, 70, 20)));
        assertFalse(claim.contains("minecraft:overworld", new BlockPos(51, 70, 40)));
        assertFalse(claim.contains("minecraft:overworld", new BlockPos(30, 70, 61)));

        // Different dimension
        assertFalse(claim.contains("minecraft:the_nether", new BlockPos(30, 70, 40)));
    }

    @Test
    public void testInvertedCoordinatesNormalized() {
        UUID owner = UUID.randomUUID();
        // Setting coords in reverse order: (50, 60) to (10, 20)
        Claim claim = new Claim("test-2", "Farm", "minecraft:overworld", 50, 60, 10, 20, owner, "Alex");

        assertEquals(10, claim.getMinX());
        assertEquals(50, claim.getMaxX());
        assertEquals(20, claim.getMinZ());
        assertEquals(60, claim.getMaxZ());
        assertTrue(claim.contains("minecraft:overworld", 25, 35));
    }

    @Test
    public void testMemberSharingPermissions() {
        UUID owner = UUID.randomUUID();
        UUID friend1 = UUID.randomUUID();
        UUID stranger = UUID.randomUUID();

        Claim claim = new Claim("test-3", "SharedBase", "minecraft:overworld", 0, 0, 100, 100, owner, "Steve");

        // Owner can access by UUID or by Name
        assertTrue(claim.isOwner(owner));
        assertTrue(claim.isOwner(UUID.randomUUID(), "Steve")); // Matches by name even with different UUID
        assertTrue(claim.canAccess(owner));
        assertTrue(claim.canAccess(UUID.randomUUID(), "Steve"));

        // Stranger cannot access
        assertFalse(claim.isMember(stranger));
        assertFalse(claim.canAccess(stranger));
        assertFalse(claim.canAccess(stranger, "Griefer"));

        // Friend added to trusted members
        claim.addMember(friend1, "FriendAlex");
        assertTrue(claim.isMember(friend1));
        assertTrue(claim.isMember(UUID.randomUUID(), "FriendAlex")); // Matches by name
        assertTrue(claim.canAccess(friend1));
        assertTrue(claim.canAccess(UUID.randomUUID(), "friendalex")); // Case insensitive
        assertFalse(claim.isOwner(friend1));

        // Friend removed by name
        assertTrue(claim.removeMember("FriendAlex"));
        assertFalse(claim.isMember(friend1));
        assertFalse(claim.canAccess(friend1));
        assertFalse(claim.canAccess(UUID.randomUUID(), "FriendAlex"));
    }

    @Test
    public void testClaimOverlaps() {
        UUID owner = UUID.randomUUID();
        Claim claimA = new Claim("c1", "A", "minecraft:overworld", 0, 0, 50, 50, owner, "P1");
        Claim claimB = new Claim("c2", "B", "minecraft:overworld", 25, 25, 75, 75, owner, "P2");
        Claim claimC = new Claim("c3", "C", "minecraft:overworld", 100, 100, 150, 150, owner, "P3");
        Claim claimNether = new Claim("c4", "D", "minecraft:the_nether", 0, 0, 50, 50, owner, "P4");

        // Overlapping
        assertTrue(claimA.overlaps(claimB));
        assertTrue(claimB.overlaps(claimA));

        // Non-overlapping
        assertFalse(claimA.overlaps(claimC));

        // Different dimension never overlaps
        assertFalse(claimA.overlaps(claimNether));
    }
}
