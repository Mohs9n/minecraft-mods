package com.simpleclaims;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.decoration.painting.Painting;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import java.util.*;

public class ClaimManager {
    private static final Map<String, Claim> CLAIMS = new HashMap<>();
    private static final Map<UUID, PlayerSelection> SELECTIONS = new HashMap<>();
    private static final Map<UUID, String> PLAYER_CLAIM_TRACKER = new HashMap<>();
    private static boolean loaded = false;

    public static class PlayerSelection {
        public BlockPos pos1;
        public BlockPos pos2;
        public String dimension;

        public PlayerSelection(String dimension) {
            this.dimension = dimension;
        }

        public boolean isComplete() {
            return pos1 != null && pos2 != null;
        }

        public int getWidthX() {
            if (!isComplete()) return 0;
            return Math.abs(pos1.getX() - pos2.getX()) + 1;
        }

        public int getWidthZ() {
            if (!isComplete()) return 0;
            return Math.abs(pos1.getZ() - pos2.getZ()) + 1;
        }

        public long getArea() {
            return (long) getWidthX() * (long) getWidthZ();
        }
    }

    public static String getDimensionId(Level level) {
        if (level == null) return "minecraft:overworld";
        return level.dimension().identifier().toString();
    }

    public static synchronized void load(ServerLevel level) {
        List<Claim> list = ClaimStorage.load(level);
        CLAIMS.clear();
        for (Claim c : list) {
            CLAIMS.put(c.getId(), c);
        }
        loaded = true;
    }

    public static synchronized void save(ServerLevel level) {
        ClaimStorage.save(level, new ArrayList<>(CLAIMS.values()));
    }

    public static synchronized List<Claim> getAllClaims() {
        return new ArrayList<>(CLAIMS.values());
    }

    public static synchronized Claim getClaimById(String id) {
        return CLAIMS.get(id);
    }

    public static synchronized List<Claim> getClaimsByOwner(UUID ownerUuid) {
        return getClaimsByOwner(ownerUuid, null);
    }

    public static synchronized List<Claim> getClaimsByOwner(Player player) {
        if (player == null) return new ArrayList<>();
        return getClaimsByOwner(player.getUUID(), player.getName().getString());
    }

    public static synchronized List<Claim> getClaimsByOwner(UUID ownerUuid, String ownerName) {
        List<Claim> result = new ArrayList<>();
        for (Claim c : CLAIMS.values()) {
            if (c.isOwner(ownerUuid, ownerName)) {
                result.add(c);
            }
        }
        return result;
    }

    public static synchronized Claim getClaimAt(String dimension, int x, int z) {
        for (Claim c : CLAIMS.values()) {
            if (c.contains(dimension, x, z)) {
                return c;
            }
        }
        return null;
    }

    public static synchronized Claim getClaimAt(Level level, BlockPos pos) {
        if (level == null || pos == null) return null;
        return getClaimAt(getDimensionId(level), pos.getX(), pos.getZ());
    }

    public static synchronized Claim findOverlappingClaim(String dimension, int x1, int z1, int x2, int z2) {
        for (Claim c : CLAIMS.values()) {
            if (c.overlaps(dimension, x1, z1, x2, z2)) {
                return c;
            }
        }
        return null;
    }

    public static synchronized boolean addClaim(ServerLevel level, Claim claim) {
        if (claim == null) return false;
        // Check for overlaps
        if (findOverlappingClaim(claim.getDimension(), claim.getMinX(), claim.getMinZ(), claim.getMaxX(), claim.getMaxZ()) != null) {
            return false;
        }
        CLAIMS.put(claim.getId(), claim);
        save(level);
        return true;
    }

    public static synchronized boolean removeClaim(ServerLevel level, Claim claim) {
        if (claim == null) return false;
        boolean removed = (CLAIMS.remove(claim.getId()) != null);
        if (removed) {
            save(level);
        }
        return removed;
    }

    public static boolean isProtected(Level level, BlockPos pos) {
        return getClaimAt(level, pos) != null;
    }

    public static boolean isOpOrAdmin(Player player) {
        if (player == null) return false;
        if (player.isCreative()) return true;
        if (player.level() != null && player.level().getServer() != null) {
            return player.level().getServer().getPlayerList().isOp(player.nameAndId());
        }
        return false;
    }

    public static boolean canPlayerModify(Player player, Level level, BlockPos pos) {
        Claim claim = getClaimAt(level, pos);
        if (claim == null) {
            return true; // Not in any claim
        }
        if (player == null) {
            return false;
        }
        if (isOpOrAdmin(player)) {
            return true; // Admin / Operator bypass
        }
        return claim.canAccess(player);
    }

    public static boolean canPlayerInteract(Player player, Level level, BlockPos pos) {
        return canPlayerModify(player, level, pos);
    }

    public static boolean canPlayerDamageEntity(Player player, Entity target) {
        if (target == null) return true;
        Level level = target.level();
        Claim claim = getClaimAt(level, target.blockPosition());
        if (claim == null) {
            return true; // Outside claims
        }
        if (player == null) {
            return false;
        }
        if (isOpOrAdmin(player)) {
            return true;
        }

        // Hostile monsters are not protected
        if (!(target instanceof Animal) && !(target instanceof Villager)
                && !(target instanceof TamableAnimal) && !(target instanceof ArmorStand)
                && !(target instanceof ItemFrame) && !(target instanceof Painting)) {
            return true;
        }

        return claim.canAccess(player);
    }

    // --- Selection Wand / Command Pos Tracking ---

    public static PlayerSelection getSelection(UUID playerUuid, String dimension) {
        return SELECTIONS.computeIfAbsent(playerUuid, u -> new PlayerSelection(dimension));
    }

    public static void setPos1(Player player, BlockPos pos) {
        String dim = getDimensionId(player.level());
        PlayerSelection sel = getSelection(player.getUUID(), dim);
        sel.dimension = dim;
        sel.pos1 = pos;
        player.sendSystemMessage(Component.literal("§6[SimpleClaims] §7Set §ePosition 1 §7to §f(" + pos.getX() + ", " + pos.getZ() + ")§7."));
        if (sel.isComplete()) {
            player.sendSystemMessage(Component.literal("§6[SimpleClaims] §7Selection size: §e" + sel.getWidthX() + "x" + sel.getWidthZ() + " §7(" + sel.getArea() + " blocks). Use §a/claim create <name> §7to claim!"));
        }
    }

    public static void setPos2(Player player, BlockPos pos) {
        String dim = getDimensionId(player.level());
        PlayerSelection sel = getSelection(player.getUUID(), dim);
        sel.dimension = dim;
        sel.pos2 = pos;
        player.sendSystemMessage(Component.literal("§6[SimpleClaims] §7Set §ePosition 2 §7to §f(" + pos.getX() + ", " + pos.getZ() + ")§7."));
        if (sel.isComplete()) {
            player.sendSystemMessage(Component.literal("§6[SimpleClaims] §7Selection size: §e" + sel.getWidthX() + "x" + sel.getWidthZ() + " §7(" + sel.getArea() + " blocks). Use §a/claim create <name> §7to claim!"));
        }
    }

    public static void clearSelection(UUID playerUuid) {
        SELECTIONS.remove(playerUuid);
    }

    // --- Border Notifications ---

    public static void onPlayerTick(ServerPlayer player) {
        UUID uuid = player.getUUID();
        Claim current = getClaimAt(player.level(), player.blockPosition());
        String currentClaimId = current != null ? current.getId() : null;
        String prevClaimId = PLAYER_CLAIM_TRACKER.get(uuid);

        if (!Objects.equals(currentClaimId, prevClaimId)) {
            PLAYER_CLAIM_TRACKER.put(uuid, currentClaimId);
            if (current != null) {
                // Entered a claim
                player.sendSystemMessage(Component.literal("§6[Claims] §7Entering §e" + current.getOwnerName() + "§7's claim §8(" + current.getName() + ")"));
            } else if (prevClaimId != null) {
                // Left a claim
                Claim prev = getClaimById(prevClaimId);
                String name = prev != null ? prev.getOwnerName() + "§7's" : "claimed";
                player.sendSystemMessage(Component.literal("§6[Claims] §7Leaving §e" + name + " territory §7(Wilderness)"));
            }
        }
    }

    public static void onPlayerDisconnect(UUID uuid) {
        PLAYER_CLAIM_TRACKER.remove(uuid);
        SELECTIONS.remove(uuid);
    }
}
