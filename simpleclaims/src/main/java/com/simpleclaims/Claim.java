package com.simpleclaims;

import net.minecraft.core.BlockPos;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class Claim {
    private String id;
    private String name;
    private String dimension;
    private int minX;
    private int maxX;
    private int minZ;
    private int maxZ;
    private String ownerUuid;
    private String ownerName;
    private Map<String, String> members; // UUID string -> Player Name
    private long createdAt;

    public Claim() {
        this.members = new HashMap<>();
    }

    public Claim(String id, String name, String dimension, int x1, int z1, int x2, int z2, UUID ownerUuid, String ownerName) {
        this.id = (id != null && !id.isBlank()) ? id : UUID.randomUUID().toString();
        this.name = (name != null && !name.isBlank()) ? name : "Claim-" + this.id.substring(0, 6);
        this.dimension = dimension != null ? dimension : "minecraft:overworld";
        this.minX = Math.min(x1, x2);
        this.maxX = Math.max(x1, x2);
        this.minZ = Math.min(z1, z2);
        this.maxZ = Math.max(z1, z2);
        this.ownerUuid = ownerUuid != null ? ownerUuid.toString() : "";
        this.ownerName = ownerName != null ? ownerName : "Unknown";
        this.members = new HashMap<>();
        this.createdAt = System.currentTimeMillis();
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDimension() {
        return dimension;
    }

    public int getMinX() {
        return minX;
    }

    public int getMaxX() {
        return maxX;
    }

    public int getMinZ() {
        return minZ;
    }

    public int getMaxZ() {
        return maxZ;
    }

    public String getOwnerUuid() {
        return ownerUuid;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public void setOwnerName(String ownerName) {
        this.ownerName = ownerName;
    }

    public Map<String, String> getMembers() {
        if (members == null) {
            members = new HashMap<>();
        }
        return members;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public boolean contains(String dim, int x, int z) {
        if (dimension == null || !dimension.equals(dim)) {
            return false;
        }
        return x >= minX && x <= maxX && z >= minZ && z <= maxZ;
    }

    public boolean contains(String dim, BlockPos pos) {
        if (pos == null) return false;
        return contains(dim, pos.getX(), pos.getZ());
    }

    public boolean overlaps(Claim other) {
        if (other == null || !this.dimension.equals(other.dimension)) {
            return false;
        }
        return this.minX <= other.maxX && this.maxX >= other.minX
                && this.minZ <= other.maxZ && this.maxZ >= other.minZ;
    }

    public boolean overlaps(String dim, int x1, int z1, int x2, int z2) {
        if (!this.dimension.equals(dim)) {
            return false;
        }
        int oMinX = Math.min(x1, x2);
        int oMaxX = Math.max(x1, x2);
        int oMinZ = Math.min(z1, z2);
        int oMaxZ = Math.max(z1, z2);
        return this.minX <= oMaxX && this.maxX >= oMinX
                && this.minZ <= oMaxZ && this.maxZ >= oMinZ;
    }

    public boolean isOwner(UUID uuid) {
        if (uuid == null || ownerUuid == null) return false;
        return ownerUuid.equals(uuid.toString());
    }

    public boolean isMember(UUID uuid) {
        if (uuid == null) return false;
        return getMembers().containsKey(uuid.toString());
    }

    public boolean canAccess(UUID uuid) {
        return isOwner(uuid) || isMember(uuid);
    }

    public void addMember(UUID uuid, String name) {
        if (uuid == null) return;
        getMembers().put(uuid.toString(), name != null ? name : "Unknown");
    }

    public boolean removeMember(UUID uuid) {
        if (uuid == null) return false;
        return getMembers().remove(uuid.toString()) != null;
    }

    public int getWidthX() {
        return maxX - minX + 1;
    }

    public int getWidthZ() {
        return maxZ - minZ + 1;
    }

    public long getArea() {
        return (long) getWidthX() * (long) getWidthZ();
    }
}
