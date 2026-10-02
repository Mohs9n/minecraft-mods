package com.simplechestshop;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.LevelResource;

import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ChestShopData {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Map<String, ShopRecord> SHOPS = new HashMap<>();
    private static boolean loaded = false;

    public static class ShopRecord {
        public String ownerUuid;
        public String ownerName;
        public long createdAt;
        public Map<String, String> trusted; // UUID string -> player name; co-managers of this shop

        public ShopRecord() {}

        public ShopRecord(UUID ownerUuid, String ownerName) {
            this.ownerUuid = ownerUuid != null ? ownerUuid.toString() : "";
            this.ownerName = ownerName != null ? ownerName : "Unknown";
            this.createdAt = System.currentTimeMillis();
            this.trusted = new HashMap<>();
        }

        // Gson deserializes via unsafe allocation and skips both the constructor and field
        // initializers, so records loaded from an older save file can have trusted == null.
        public Map<String, String> getTrusted() {
            if (trusted == null) {
                trusted = new HashMap<>();
            }
            return trusted;
        }
    }

    private static String posKey(BlockPos pos) {
        return pos.getX() + "," + pos.getY() + "," + pos.getZ();
    }

    public static synchronized void load(ServerLevel level) {
        try {
            Path file = getStoragePath(level);
            if (Files.exists(file)) {
                try (Reader reader = Files.newBufferedReader(file)) {
                    Type type = new TypeToken<Map<String, ShopRecord>>() {}.getType();
                    Map<String, ShopRecord> map = GSON.fromJson(reader, type);
                    if (map != null) {
                        SHOPS.clear();
                        SHOPS.putAll(map);
                    }
                }
            }
            loaded = true;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static synchronized void save(ServerLevel level) {
        try {
            Path file = getStoragePath(level);
            Files.createDirectories(file.getParent());
            // Write to a temp file first and atomically swap it in, so a crash mid-write
            // (or a full disk) can never leave chest_shops.json half-written/corrupted.
            Path tmp = file.resolveSibling(file.getFileName().toString() + ".tmp");
            try (Writer writer = Files.newBufferedWriter(tmp)) {
                GSON.toJson(SHOPS, writer);
            }
            try {
                Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (java.nio.file.AtomicMoveNotSupportedException e) {
                Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static Path getStoragePath(ServerLevel level) {
        return level.getServer().getWorldPath(LevelResource.ROOT).resolve("chest_shops.json");
    }

    public static synchronized ShopRecord getRecord(BlockPos pos) {
        return SHOPS.get(posKey(pos));
    }

    public static synchronized ShopRecord getRecord(Level level, BlockPos pos) {
        ShopRecord record = SHOPS.get(posKey(pos));
        if (record != null) return record;
        if (level != null && pos != null) {
            BlockState state = level.getBlockState(pos);
            if (state.getBlock() instanceof ChestBlock) {
                BlockPos other = ChestBlock.getConnectedBlockPos(pos, state);
                if (other != null && !other.equals(pos)) {
                    return SHOPS.get(posKey(other));
                }
            }
        }
        return null;
    }

    public static synchronized void register(ServerLevel level, BlockPos pos, UUID ownerUuid, String ownerName) {
        ShopRecord record = new ShopRecord(ownerUuid, ownerName);
        SHOPS.put(posKey(pos), record);

        if (level != null && pos != null) {
            BlockState state = level.getBlockState(pos);
            if (state.getBlock() instanceof ChestBlock) {
                BlockPos other = ChestBlock.getConnectedBlockPos(pos, state);
                if (other != null && !other.equals(pos)) {
                    SHOPS.put(posKey(other), record);
                }
            }
        }
        save(level);
    }

    public static synchronized void remove(ServerLevel level, BlockPos pos) {
        boolean changed = (SHOPS.remove(posKey(pos)) != null);
        if (level != null && pos != null) {
            BlockState state = level.getBlockState(pos);
            if (state.getBlock() instanceof ChestBlock) {
                BlockPos other = ChestBlock.getConnectedBlockPos(pos, state);
                if (other != null && !other.equals(pos)) {
                    if (SHOPS.remove(posKey(other)) != null) {
                        changed = true;
                    }
                }
            }
        }
        if (changed) {
            save(level);
        }
    }

    public static boolean isOwner(BlockPos pos, UUID playerUuid) {
        ShopRecord record = getRecord(pos);
        if (record == null || record.ownerUuid == null || record.ownerUuid.isBlank()) {
            return false;
        }
        return record.ownerUuid.equals(playerUuid.toString());
    }

    public static synchronized boolean addTrusted(ServerLevel level, BlockPos pos, UUID uuid, String name) {
        ShopRecord record = getRecord(level, pos);
        if (record == null || uuid == null) return false;
        record.getTrusted().put(uuid.toString(), name != null ? name : "Unknown");
        save(level);
        return true;
    }

    public static synchronized boolean removeTrusted(ServerLevel level, BlockPos pos, UUID uuid) {
        ShopRecord record = getRecord(level, pos);
        if (record == null || uuid == null) return false;
        boolean removed = record.getTrusted().remove(uuid.toString()) != null;
        if (removed) {
            save(level);
        }
        return removed;
    }

    public static Map<String, String> getTrustedNames(Level level, BlockPos pos) {
        ShopRecord record = getRecord(level, pos);
        if (record == null) return java.util.Collections.emptyMap();
        return record.getTrusted();
    }

    public static boolean isTrusted(Level level, BlockPos pos, UUID uuid) {
        if (uuid == null) return false;
        ShopRecord record = getRecord(level, pos);
        return record != null && record.getTrusted().containsKey(uuid.toString());
    }

    /**
     * True if the player is the owner OR a trusted co-manager of the shop at pos.
     */
    public static boolean canManage(Level level, BlockPos pos, UUID uuid) {
        if (uuid == null) return false;
        ShopRecord record = getRecord(level, pos);
        if (record == null) return false;
        if (uuid.toString().equals(record.ownerUuid)) return true;
        return record.getTrusted().containsKey(uuid.toString());
    }
}
