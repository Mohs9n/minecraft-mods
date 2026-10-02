package com.simpleclaims;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.level.Level;

import java.util.*;

public class ClaimManager {
    private static final Map<String, Claim> CLAIMS = new HashMap<>();
    private static final Map<UUID, PlayerSelection> SELECTIONS = new HashMap<>();
    private static final Map<UUID, String> PLAYER_CLAIM_TRACKER = new HashMap<>();
    private static boolean loaded = false;

    // Per-dimension chunk-bucket spatial index: dimension -> chunkKey -> claim ids touching
    // that chunk. Lets getClaimAt/findOverlappingClaim check only the handful of claims near
    // a point instead of scanning every claim on the server on every block interaction/tick.
    private static final Map<String, Map<Long, Set<String>>> CHUNK_INDEX = new HashMap<>();

    // Single source of truth for the wand's exact name/lore. isClaimWand() requires an
    // exact match on both (not a name substring), so renaming an arbitrary golden hoe in
    // an anvil can never forge wand powers - anvils can only edit CUSTOM_NAME, not LORE.
    public static final Component WAND_NAME = Component.literal("§6§lClaim Wand");
    public static final List<Component> WAND_LORE = List.of(
            Component.literal("§7Left-click block: §eSet Corner 1"),
            Component.literal("§7Right-click block: §eSet Corner 2"),
            Component.literal("§7Use §a/claim create <name> §7to finish!")
    );

    public static boolean isClaimWand(ItemStack stack) {
        if (stack == null || stack.isEmpty() || stack.getItem() != Items.GOLDEN_HOE) {
            return false;
        }
        Component name = stack.get(DataComponents.CUSTOM_NAME);
        if (name == null || !name.getString().equals(WAND_NAME.getString())) {
            return false;
        }
        ItemLore lore = stack.get(DataComponents.LORE);
        if (lore == null) {
            return false;
        }
        List<Component> lines = lore.lines();
        if (lines.size() != WAND_LORE.size()) {
            return false;
        }
        for (int i = 0; i < lines.size(); i++) {
            if (!lines.get(i).getString().equals(WAND_LORE.get(i).getString())) {
                return false;
            }
        }
        return true;
    }

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
        CHUNK_INDEX.clear();
        for (Claim c : list) {
            CLAIMS.put(c.getId(), c);
            indexClaim(c);
        }
        loaded = true;
    }

    private static long chunkKey(int chunkX, int chunkZ) {
        return (((long) chunkX) << 32) | (chunkZ & 0xFFFFFFFFL);
    }

    private static void indexClaim(Claim claim) {
        Map<Long, Set<String>> dimIndex = CHUNK_INDEX.computeIfAbsent(claim.getDimension(), d -> new HashMap<>());
        int minCx = claim.getMinX() >> 4, maxCx = claim.getMaxX() >> 4;
        int minCz = claim.getMinZ() >> 4, maxCz = claim.getMaxZ() >> 4;
        for (int cx = minCx; cx <= maxCx; cx++) {
            for (int cz = minCz; cz <= maxCz; cz++) {
                dimIndex.computeIfAbsent(chunkKey(cx, cz), k -> new HashSet<>()).add(claim.getId());
            }
        }
    }

    private static void unindexClaim(Claim claim) {
        Map<Long, Set<String>> dimIndex = CHUNK_INDEX.get(claim.getDimension());
        if (dimIndex == null) return;
        int minCx = claim.getMinX() >> 4, maxCx = claim.getMaxX() >> 4;
        int minCz = claim.getMinZ() >> 4, maxCz = claim.getMaxZ() >> 4;
        for (int cx = minCx; cx <= maxCx; cx++) {
            for (int cz = minCz; cz <= maxCz; cz++) {
                long key = chunkKey(cx, cz);
                Set<String> ids = dimIndex.get(key);
                if (ids != null) {
                    ids.remove(claim.getId());
                    if (ids.isEmpty()) {
                        dimIndex.remove(key);
                    }
                }
            }
        }
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
        List<Claim> result = new ArrayList<>();
        if (ownerUuid == null) return result;
        String uuidStr = ownerUuid.toString();
        for (Claim c : CLAIMS.values()) {
            if (uuidStr.equals(c.getOwnerUuid())) {
                result.add(c);
            }
        }
        return result;
    }

    public static synchronized Claim getClaimAt(String dimension, int x, int z) {
        Map<Long, Set<String>> dimIndex = CHUNK_INDEX.get(dimension);
        if (dimIndex == null) return null;
        Set<String> ids = dimIndex.get(chunkKey(x >> 4, z >> 4));
        if (ids == null) return null;
        for (String id : ids) {
            Claim c = CLAIMS.get(id);
            if (c != null && c.contains(dimension, x, z)) {
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
        Map<Long, Set<String>> dimIndex = CHUNK_INDEX.get(dimension);
        if (dimIndex == null) return null;
        int minX = Math.min(x1, x2), maxX = Math.max(x1, x2);
        int minZ = Math.min(z1, z2), maxZ = Math.max(z1, z2);
        int minCx = minX >> 4, maxCx = maxX >> 4;
        int minCz = minZ >> 4, maxCz = maxZ >> 4;
        Set<String> checked = new HashSet<>();
        for (int cx = minCx; cx <= maxCx; cx++) {
            for (int cz = minCz; cz <= maxCz; cz++) {
                Set<String> ids = dimIndex.get(chunkKey(cx, cz));
                if (ids == null) continue;
                for (String id : ids) {
                    if (!checked.add(id)) continue;
                    Claim c = CLAIMS.get(id);
                    if (c != null && c.overlaps(dimension, x1, z1, x2, z2)) {
                        return c;
                    }
                }
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
        indexClaim(claim);
        save(level);
        return true;
    }

    public static synchronized boolean removeClaim(ServerLevel level, Claim claim) {
        if (claim == null) return false;
        boolean removed = (CLAIMS.remove(claim.getId()) != null);
        if (removed) {
            unindexClaim(claim);
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
        return claim.canAccess(player.getUUID());
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

        return claim.canAccess(player.getUUID());
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

    // Standing right on a claim's edge can flicker the detected claim in/out every tick.
    // A transition is only announced once the new state has held steady for this many
    // ticks (20 ticks = 1 second), so border jitter no longer spams chat.
    private static final int BORDER_DEBOUNCE_TICKS = 10;
    private static final Map<UUID, String> PENDING_CLAIM_STATE = new HashMap<>();
    private static final Map<UUID, Integer> PENDING_CLAIM_TICKS = new HashMap<>();

    public static void onPlayerTick(ServerPlayer player) {
        UUID uuid = player.getUUID();
        Claim current = getClaimAt(player.level(), player.blockPosition());
        String currentClaimId = current != null ? current.getId() : null;
        String confirmedClaimId = PLAYER_CLAIM_TRACKER.get(uuid);

        if (Objects.equals(currentClaimId, confirmedClaimId)) {
            // Back to the last confirmed state - cancel any pending transition (this is
            // exactly what absorbs border-line jitter instead of announcing it).
            PENDING_CLAIM_STATE.remove(uuid);
            PENDING_CLAIM_TICKS.remove(uuid);
            return;
        }

        String pendingClaimId = PENDING_CLAIM_STATE.get(uuid);
        if (!Objects.equals(currentClaimId, pendingClaimId)) {
            // A new candidate state - start (or restart) the debounce timer for it.
            PENDING_CLAIM_STATE.put(uuid, currentClaimId);
            PENDING_CLAIM_TICKS.put(uuid, 1);
            return;
        }

        int heldTicks = PENDING_CLAIM_TICKS.merge(uuid, 1, Integer::sum);
        if (heldTicks < BORDER_DEBOUNCE_TICKS) {
            return;
        }

        // The new state has held steady long enough - confirm and announce it.
        PENDING_CLAIM_STATE.remove(uuid);
        PENDING_CLAIM_TICKS.remove(uuid);
        PLAYER_CLAIM_TRACKER.put(uuid, currentClaimId);

        if (current != null) {
            player.sendSystemMessage(Component.literal("§6[Claims] §7Entering §e" + current.getOwnerName() + "§7's claim §8(" + current.getName() + ")"));
        } else if (confirmedClaimId != null) {
            Claim prev = getClaimById(confirmedClaimId);
            String name = prev != null ? prev.getOwnerName() + "§7's" : "claimed";
            player.sendSystemMessage(Component.literal("§6[Claims] §7Leaving §e" + name + " territory §7(Wilderness)"));
        }
    }

    public static void onPlayerDisconnect(UUID uuid) {
        PLAYER_CLAIM_TRACKER.remove(uuid);
        PENDING_CLAIM_STATE.remove(uuid);
        PENDING_CLAIM_TICKS.remove(uuid);
        SELECTIONS.remove(uuid);
    }
}
