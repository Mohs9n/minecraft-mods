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

        public ShopRecord() {}

        public ShopRecord(UUID ownerUuid, String ownerName) {
            this.ownerUuid = ownerUuid != null ? ownerUuid.toString() : "";
            this.ownerName = ownerName != null ? ownerName : "Unknown";
            this.createdAt = System.currentTimeMillis();
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
            try (Writer writer = Files.newBufferedWriter(file)) {
                GSON.toJson(SHOPS, writer);
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
}
