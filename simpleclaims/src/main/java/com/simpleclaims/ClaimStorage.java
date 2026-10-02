package com.simpleclaims;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.storage.LevelResource;

import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

public class ClaimStorage {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static Path getStoragePath(ServerLevel level) {
        return level.getServer().getWorldPath(LevelResource.ROOT).resolve("simple_claims.json");
    }

    public static synchronized List<Claim> load(ServerLevel level) {
        List<Claim> list = new ArrayList<>();
        try {
            Path file = getStoragePath(level);
            if (Files.exists(file)) {
                try (Reader reader = Files.newBufferedReader(file)) {
                    Type type = new TypeToken<List<Claim>>() {}.getType();
                    List<Claim> loaded = GSON.fromJson(reader, type);
                    if (loaded != null) {
                        list.addAll(loaded);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    public static synchronized void save(ServerLevel level, List<Claim> claims) {
        try {
            Path file = getStoragePath(level);
            Files.createDirectories(file.getParent());
            // Write to a temp file first and atomically swap it in, so a crash mid-write
            // (or a full disk) can never leave simple_claims.json half-written/corrupted.
            Path tmp = file.resolveSibling(file.getFileName().toString() + ".tmp");
            try (Writer writer = Files.newBufferedWriter(tmp)) {
                GSON.toJson(claims, writer);
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
}
