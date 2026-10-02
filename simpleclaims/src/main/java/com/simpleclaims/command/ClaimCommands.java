package com.simpleclaims.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.simpleclaims.Claim;
import com.simpleclaims.ClaimManager;
import com.simpleclaims.gui.ClaimListMenu;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Prediction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemLore;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ClaimCommands {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("claim")
                .executes(ctx -> {
                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                    ClaimListMenu.open(player);
                    return 1;
                })
                .then(Commands.literal("help")
                        .executes(ctx -> {
                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                            return showHelp(player);
                        }))
                .then(Commands.literal("wand")
                        .executes(ctx -> {
                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                            return giveWand(player);
                        }))
                .then(Commands.literal("gui")
                        .executes(ctx -> {
                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                            ClaimListMenu.open(player);
                            return 1;
                        }))
                .then(Commands.literal("menu")
                        .executes(ctx -> {
                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                            ClaimListMenu.open(player);
                            return 1;
                        }))
                .then(Commands.literal("pos1")
                        .executes(ctx -> {
                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                            ClaimManager.setPos1(player, player.blockPosition());
                            return 1;
                        }))
                .then(Commands.literal("pos2")
                        .executes(ctx -> {
                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                            ClaimManager.setPos2(player, player.blockPosition());
                            return 1;
                        }))
                .then(Commands.literal("create")
                        .then(Commands.argument("name", StringArgumentType.word())
                                .executes(ctx -> {
                                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                                    String name = StringArgumentType.getString(ctx, "name");
                                    return handleCreateFromSelection(player, name);
                                })
                                .then(Commands.argument("x1", IntegerArgumentType.integer())
                                        .then(Commands.argument("z1", IntegerArgumentType.integer())
                                                .then(Commands.argument("x2", IntegerArgumentType.integer())
                                                        .then(Commands.argument("z2", IntegerArgumentType.integer())
                                                                .executes(ctx -> {
                                                                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                                                                    String name = StringArgumentType.getString(ctx, "name");
                                                                    int x1 = IntegerArgumentType.getInteger(ctx, "x1");
                                                                    int z1 = IntegerArgumentType.getInteger(ctx, "z1");
                                                                    int x2 = IntegerArgumentType.getInteger(ctx, "x2");
                                                                    int z2 = IntegerArgumentType.getInteger(ctx, "z2");
                                                                    return handleCreateWithCoords(player, name, x1, z1, x2, z2);
                                                                })))))))
                .then(Commands.literal("trust")
                        .then(Commands.argument("player", StringArgumentType.word())
                                .executes(ctx -> {
                                    ServerPlayer owner = ctx.getSource().getPlayerOrException();
                                    String targetName = StringArgumentType.getString(ctx, "player");
                                    ServerPlayer target = null;
                                    if (owner.level().getServer() != null) {
                                        target = owner.level().getServer().getPlayerList().getPlayerByName(targetName);
                                    }
                                    if (target != null) {
                                        return handleTrust(owner, target.getUUID(), target.getName().getString());
                                    } else {
                                        // Offline player trust
                                        UUID offlineUuid = UUID.nameUUIDFromBytes(("OfflinePlayer:" + targetName).getBytes(java.nio.charset.StandardCharsets.UTF_8));
                                        return handleTrust(owner, offlineUuid, targetName);
                                    }
                                })))
                .then(Commands.literal("untrust")
                        .then(Commands.argument("player", StringArgumentType.word())
                                .executes(ctx -> {
                                    ServerPlayer owner = ctx.getSource().getPlayerOrException();
                                    String targetName = StringArgumentType.getString(ctx, "player");
                                    return handleUntrust(owner, targetName);
                                })))
                .then(Commands.literal("list")
                        .executes(ctx -> {
                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                            return handleList(player);
                        }))
                .then(Commands.literal("info")
                        .executes(ctx -> {
                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                            return showClaimInfo(player);
                        }))
                .then(Commands.literal("delete")
                        .executes(ctx -> {
                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                            return handleDelete(player);
                        }))
                .then(Commands.literal("abandon")
                        .executes(ctx -> {
                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                            return handleDelete(player);
                        }))
                .then(Commands.literal("forceload")
                        .executes(ctx -> {
                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                            return handleToggleForceLoad(player);
                        }))
                .then(Commands.literal("adminbypass")
                        .executes(ctx -> {
                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                            return handleToggleAdminBypass(player);
                        }))
        );
    }

    private static int showHelp(ServerPlayer player) {
        player.sendSystemMessage(Component.literal("§6================ §e[SimpleClaims Help] §6================"));
        player.sendSystemMessage(Component.literal("§e/claim gui §7- Opens a graphical menu to manage your claims"));
        player.sendSystemMessage(Component.literal("§e/claim wand §7- Gives a selection wand to set corners"));
        player.sendSystemMessage(Component.literal("§e/claim pos1 §7- Sets Corner 1 to your position"));
        player.sendSystemMessage(Component.literal("§e/claim pos2 §7- Sets Corner 2 to your position"));
        player.sendSystemMessage(Component.literal("§e/claim create <name> §7- Claims the selected area (all Y levels)"));
        player.sendSystemMessage(Component.literal("§e/claim trust <player> §7- Shares the claim with a friend"));
        player.sendSystemMessage(Component.literal("§e/claim untrust <player> §7- Removes a shared friend"));
        player.sendSystemMessage(Component.literal("§e/claim list §7- Lists all your claims"));
        player.sendSystemMessage(Component.literal("§e/claim info §7- Shows info of claim you are standing in"));
        player.sendSystemMessage(Component.literal("§e/claim delete §7- Deletes the claim you are standing in"));
        player.sendSystemMessage(Component.literal("§e/claim forceload §7- Toggles keeping the claim loaded while offline"));
        if (ClaimManager.isOpOrAdmin(player)) {
            player.sendSystemMessage(Component.literal("§e/claim adminbypass §7- (Op) Toggles ignoring claim protection"));
        }
        player.sendSystemMessage(Component.literal("§6==================================================="));
        return 1;
    }

    public static int giveWand(ServerPlayer player) {
        ItemStack wand = new ItemStack(Items.GOLDEN_HOE);
        wand.set(DataComponents.CUSTOM_NAME, ClaimManager.WAND_NAME);
        wand.set(DataComponents.LORE, new ItemLore(ClaimManager.WAND_LORE));

        if (!player.getInventory().add(wand)) {
            player.drop(wand, false, Prediction.SERVER_ONLY);
        }
        player.sendSystemMessage(Component.literal("§a✔ Received Claim Wand! Left-click a block for Corner 1, Right-click for Corner 2."));
        return 1;
    }

    /**
     * Entry point for the GUI (ClaimNameInputMenu) to create a claim from the player's
     * current wand selection without going through the chat/command path.
     */
    public static int createClaimFromSelectionGui(ServerPlayer player, String name) {
        return handleCreateFromSelection(player, name);
    }

    private static int handleCreateFromSelection(ServerPlayer player, String name) {
        String dim = ClaimManager.getDimensionId(player.level());
        ClaimManager.PlayerSelection sel = ClaimManager.getSelection(player.getUUID(), dim);

        if (!sel.isComplete()) {
            player.sendSystemMessage(Component.literal("§c[SimpleClaims] Please set both corners first! Use §e/claim wand §cor §e/claim pos1 §cand §e/claim pos2§c."));
            return 0;
        }

        return createClaimInternal(player, name, dim, sel.pos1.getX(), sel.pos1.getZ(), sel.pos2.getX(), sel.pos2.getZ());
    }

    private static int handleCreateWithCoords(ServerPlayer player, String name, int x1, int z1, int x2, int z2) {
        String dim = ClaimManager.getDimensionId(player.level());
        return createClaimInternal(player, name, dim, x1, z1, x2, z2);
    }

    private static int createClaimInternal(ServerPlayer player, String name, String dim, int x1, int z1, int x2, int z2) {
        int minX = Math.min(x1, x2);
        int maxX = Math.max(x1, x2);
        int minZ = Math.min(z1, z2);
        int maxZ = Math.max(z1, z2);

        int widthX = maxX - minX + 1;
        int widthZ = maxZ - minZ + 1;

        if (widthX < 3 || widthZ < 3) {
            player.sendSystemMessage(Component.literal("§c[SimpleClaims] Claims must be at least 3x3 blocks wide!"));
            return 0;
        }

        // Check overlaps
        Claim overlap = ClaimManager.findOverlappingClaim(dim, minX, minZ, maxX, maxZ);
        if (overlap != null) {
            player.sendSystemMessage(Component.literal("§c[SimpleClaims] Cannot create claim: Overlaps with §e" + overlap.getOwnerName() + "§c's claim (§f" + overlap.getName() + "§c)!"));
            return 0;
        }

        // Per-player claim limits (skipped for ops), inspired by FTB Chunks' claim-power
        // system - stops a single player from claiming the whole map.
        long area = (long) widthX * widthZ;
        if (!ClaimManager.isOpOrAdmin(player)) {
            ClaimManager.ClaimLimitCheck limit = ClaimManager.checkClaimLimit(player.getUUID(), area);
            if (!limit.allowed) {
                player.sendSystemMessage(Component.literal("§c[SimpleClaims] " + limit.reason));
                return 0;
            }
        }

        Claim claim = new Claim(null, name, dim, minX, minZ, maxX, maxZ, player.getUUID(), player.getName().getString());
        ServerLevel sLevel = (ServerLevel) player.level();
        if (ClaimManager.addClaim(sLevel, claim)) {
            ClaimManager.clearSelection(player.getUUID());
            player.sendSystemMessage(Component.literal("§a✔ Created claim §e\"" + name + "\"§a!"));
            player.sendSystemMessage(Component.literal("§7Area: §f" + widthX + "x" + widthZ + " §7(" + claim.getArea() + " blocks) from bedrock to sky limit!"));
            player.sendSystemMessage(Component.literal("§7Use §b/claim trust <player> §7to share this claim with friends."));
            return 1;
        } else {
            player.sendSystemMessage(Component.literal("§c[SimpleClaims] Failed to save claim."));
            return 0;
        }
    }

    private static int handleTrust(ServerPlayer owner, UUID targetUuid, String targetName) {
        Claim claim = getRelevantClaim(owner);
        if (claim == null) {
            owner.sendSystemMessage(Component.literal("§c[SimpleClaims] You must be standing in your claim (or have at least one claim) to trust players!"));
            return 0;
        }

        if (!claim.isOwner(owner.getUUID(), owner.getName().getString()) && !ClaimManager.isOpOrAdmin(owner)) {
            owner.sendSystemMessage(Component.literal("§c[SimpleClaims] Only the owner of this claim can trust players!"));
            return 0;
        }

        if (claim.isOwner(targetUuid, targetName)) {
            owner.sendSystemMessage(Component.literal("§c[SimpleClaims] You are already the owner of this claim!"));
            return 0;
        }

        claim.addMember(targetUuid, targetName);
        ClaimManager.save((ServerLevel) owner.level());

        owner.sendSystemMessage(Component.literal("§a✔ Trusted §e" + targetName + " §ain claim §6" + claim.getName() + "§a! They now have full build & chest access."));
        if (owner.level().getServer() != null) {
            ServerPlayer targetPlayer = owner.level().getServer().getPlayerList().getPlayer(targetUuid);
            if (targetPlayer != null) {
                targetPlayer.sendSystemMessage(Component.literal("§a✔ You were trusted in §e" + owner.getName().getString() + "§a's claim (§6" + claim.getName() + "§a)!"));
            }
        }
        return 1;
    }

    private static int handleUntrust(ServerPlayer owner, String targetName) {
        Claim claim = getRelevantClaim(owner);
        if (claim == null) {
            owner.sendSystemMessage(Component.literal("§c[SimpleClaims] You must be standing in your claim to untrust players!"));
            return 0;
        }

        if (!claim.isOwner(owner.getUUID(), owner.getName().getString()) && !ClaimManager.isOpOrAdmin(owner)) {
            owner.sendSystemMessage(Component.literal("§c[SimpleClaims] Only the owner of this claim can untrust players!"));
            return 0;
        }

        boolean removed = claim.removeMember(targetName);
        if (!removed) {
            owner.sendSystemMessage(Component.literal("§c[SimpleClaims] Player §e" + targetName + " §cis not trusted in this claim."));
            return 0;
        }

        ClaimManager.save((ServerLevel) owner.level());
        owner.sendSystemMessage(Component.literal("§e✔ Removed §f" + targetName + " §efrom claim §6" + claim.getName() + "§e."));
        return 1;
    }

    private static int handleList(ServerPlayer player) {
        List<Claim> claims = ClaimManager.getClaimsByOwner(player);
        if (claims.isEmpty()) {
            player.sendSystemMessage(Component.literal("§e[SimpleClaims] You do not own any claims yet. Use §a/claim wand §cto create one!"));
            return 1;
        }

        player.sendSystemMessage(Component.literal("§6================ §e[Your Claims (" + claims.size() + ")] §6================"));
        for (Claim c : claims) {
            player.sendSystemMessage(Component.literal("§8• §e" + c.getName() + " §7(" + c.getDimension() + "): §f("
                    + c.getMinX() + ", " + c.getMinZ() + ") §7to §f(" + c.getMaxX() + ", " + c.getMaxZ() + ") §8[" + c.getArea() + " blocks]"));
            if (!c.getMembers().isEmpty()) {
                player.sendSystemMessage(Component.literal("   §7Trusted: §a" + String.join(", ", c.getMembers().values())));
            }
        }
        player.sendSystemMessage(Component.literal("§6================================================="));
        return 1;
    }

    private static int showClaimInfo(ServerPlayer player) {
        Claim claim = ClaimManager.getClaimAt(player.level(), player.blockPosition());
        if (claim == null) {
            player.sendSystemMessage(Component.literal("§7[SimpleClaims] You are currently in the §2Wilderness §7(Unclaimed land)."));
            return 1;
        }

        player.sendSystemMessage(Component.literal("§6================ §e[Claim Info] §6================"));
        player.sendSystemMessage(Component.literal("§7Name: §f" + claim.getName()));
        player.sendSystemMessage(Component.literal("§7Owner: §e" + claim.getOwnerName()));
        player.sendSystemMessage(Component.literal("§7Bounds: §f(" + claim.getMinX() + ", " + claim.getMinZ() + ") §7to §f(" + claim.getMaxX() + ", " + claim.getMaxZ() + ")"));
        player.sendSystemMessage(Component.literal("§7Height: §aAll Y levels (Bedrock to Sky)"));
        player.sendSystemMessage(Component.literal("§7Size: §b" + claim.getWidthX() + "x" + claim.getWidthZ() + " §7(" + claim.getArea() + " blocks)"));
        if (!claim.getMembers().isEmpty()) {
            player.sendSystemMessage(Component.literal("§7Trusted Members: §a" + String.join(", ", claim.getMembers().values())));
        } else {
            player.sendSystemMessage(Component.literal("§7Trusted Members: §8None"));
        }
        player.sendSystemMessage(Component.literal("§6==============================================="));
        return 1;
    }

    private static int handleDelete(ServerPlayer player) {
        Claim claim = ClaimManager.getClaimAt(player.level(), player.blockPosition());
        if (claim == null) {
            player.sendSystemMessage(Component.literal("§c[SimpleClaims] You must be standing in the claim you want to delete!"));
            return 0;
        }

        if (!claim.isOwner(player.getUUID(), player.getName().getString()) && !ClaimManager.isOpOrAdmin(player)) {
            player.sendSystemMessage(Component.literal("§c[SimpleClaims] You do not have permission to delete §e" + claim.getOwnerName() + "§c's claim!"));
            return 0;
        }

        String claimName = claim.getName();
        ServerLevel sLevel = (ServerLevel) player.level();
        if (ClaimManager.removeClaim(sLevel, claim)) {
            player.sendSystemMessage(Component.literal("§a✔ Successfully deleted claim §e\"" + claimName + "\"§a. The land is now wilderness."));
            return 1;
        } else {
            player.sendSystemMessage(Component.literal("§c[SimpleClaims] Failed to delete claim."));
            return 0;
        }
    }

    private static int handleToggleForceLoad(ServerPlayer player) {
        Claim claim = ClaimManager.getClaimAt(player.level(), player.blockPosition());
        if (claim == null) {
            player.sendSystemMessage(Component.literal("§c[SimpleClaims] You must be standing in the claim you want to force-load!"));
            return 0;
        }

        if (!claim.isOwner(player.getUUID(), player.getName().getString()) && !ClaimManager.isOpOrAdmin(player)) {
            player.sendSystemMessage(Component.literal("§c[SimpleClaims] Only the owner of this claim can toggle force-loading!"));
            return 0;
        }

        ServerLevel sLevel = (ServerLevel) player.level();
        if (claim.isForceLoaded()) {
            claim.setForceLoaded(false);
            ClaimManager.applyForceLoad(sLevel, claim, false);
            ClaimManager.save(sLevel);
            player.sendSystemMessage(Component.literal("§e✔ §6" + claim.getName() + " §eis no longer force-loaded; it will unload like a normal chunk when everyone leaves."));
            return 1;
        }

        long chunksNeeded = ((long) (claim.getMaxX() >> 4) - (claim.getMinX() >> 4) + 1)
                * ((long) (claim.getMaxZ() >> 4) - (claim.getMinZ() >> 4) + 1);
        long alreadyForced = ClaimManager.countForceLoadedChunks(player.getUUID());
        if (!ClaimManager.isOpOrAdmin(player) && alreadyForced + chunksNeeded > ClaimManager.MAX_FORCELOADED_CHUNKS_PER_PLAYER) {
            player.sendSystemMessage(Component.literal("§c[SimpleClaims] That would force-load " + chunksNeeded + " chunks, but you can only force-load "
                    + ClaimManager.MAX_FORCELOADED_CHUNKS_PER_PLAYER + " total (currently using " + alreadyForced + ")!"));
            return 0;
        }

        claim.setForceLoaded(true);
        ClaimManager.applyForceLoad(sLevel, claim, true);
        ClaimManager.save(sLevel);
        player.sendSystemMessage(Component.literal("§a✔ §6" + claim.getName() + " §awill now stay loaded and keep ticking even while you're offline!"));
        return 1;
    }

    private static int handleToggleAdminBypass(ServerPlayer player) {
        Boolean newState = ClaimManager.toggleAdminBypass(player);
        if (newState == null) {
            player.sendSystemMessage(Component.literal("§c[SimpleClaims] Only server operators can use admin bypass."));
            return 0;
        }
        if (newState) {
            player.sendSystemMessage(Component.literal("§c⚠ Admin bypass §lENABLED§r§c - you now ignore all claim protection. Toggle it off when you're done!"));
        } else {
            player.sendSystemMessage(Component.literal("§a✔ Admin bypass disabled - you're subject to claim protection like everyone else again."));
        }
        return 1;
    }

    private static Claim getRelevantClaim(ServerPlayer player) {
        // Try claim at current position first
        Claim claim = ClaimManager.getClaimAt(player.level(), player.blockPosition());
        if (claim != null) return claim;

        // Otherwise if player owns exactly 1 claim, return that one
        List<Claim> owned = ClaimManager.getClaimsByOwner(player);
        if (owned.size() == 1) {
            return owned.get(0);
        }
        return null;
    }
}
