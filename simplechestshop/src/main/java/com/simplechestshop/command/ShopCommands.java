package com.simplechestshop.command;

import com.mojang.brigadier.CommandDispatcher;
import com.simplechestshop.ChestShopData;
import com.simplechestshop.gui.ShopCreationMenu;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.Map;
import java.util.UUID;

public class ShopCommands {

    private static final double TRUST_REACH = 6.0;

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        // /shopcreate
        dispatcher.register(Commands.literal("shopcreate")
                .executes(context -> {
                    ServerPlayer player = context.getSource().getPlayerOrException();
                    ShopCreationMenu.open(player);
                    return 1;
                })
        );

        // /shop and subcommands
        dispatcher.register(Commands.literal("shop")
                .then(Commands.literal("create")
                        .executes(context -> {
                            ServerPlayer player = context.getSource().getPlayerOrException();
                            ShopCreationMenu.open(player);
                            return 1;
                        })
                )
                .then(Commands.literal("help")
                        .executes(context -> {
                            ServerPlayer player = context.getSource().getPlayerOrException();
                            sendHelp(player);
                            return 1;
                        })
                )
                .then(Commands.literal("trust")
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(context -> {
                                    ServerPlayer owner = context.getSource().getPlayerOrException();
                                    ServerPlayer target = EntityArgument.getPlayer(context, "player");
                                    return handleTrust(owner, target.getUUID(), target.getName().getString());
                                })
                        )
                )
                .then(Commands.literal("untrust")
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(context -> {
                                    ServerPlayer owner = context.getSource().getPlayerOrException();
                                    ServerPlayer target = EntityArgument.getPlayer(context, "player");
                                    return handleUntrust(owner, target.getUUID(), target.getName().getString());
                                })
                        )
                )
                .then(Commands.literal("trusted")
                        .executes(context -> {
                            ServerPlayer player = context.getSource().getPlayerOrException();
                            return handleListTrusted(player);
                        })
                )
                .executes(context -> {
                    ServerPlayer player = context.getSource().getPlayerOrException();
                    ShopCreationMenu.open(player);
                    return 1;
                })
        );
    }

    /**
     * Raycasts along the player's exact view to find a chest within reach, so shop trust
     * commands operate on "the shop chest you're looking at" without needing coordinates.
     */
    private static BlockPos getTargetedChest(ServerPlayer player) {
        Vec3 start = player.getEyePosition();
        Vec3 look = player.getViewVector(1.0F);
        Vec3 end = start.add(look.x * TRUST_REACH, look.y * TRUST_REACH, look.z * TRUST_REACH);
        ClipContext ctx = new ClipContext(start, end, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player);
        BlockHitResult hit = player.level().clip(ctx);
        if (hit.getType() != HitResult.Type.BLOCK) {
            return null;
        }
        BlockPos pos = hit.getBlockPos();
        if (!(player.level().getBlockState(pos).getBlock() instanceof ChestBlock)) {
            return null;
        }
        return pos;
    }

    private static int handleTrust(ServerPlayer owner, UUID targetUuid, String targetName) {
        BlockPos pos = getTargetedChest(owner);
        if (pos == null) {
            owner.sendSystemMessage(Component.literal("§c[Chest Shop] Look directly at your shop chest (within 6 blocks) to trust someone in it!"));
            return 0;
        }

        ChestShopData.ShopRecord record = ChestShopData.getRecord(owner.level(), pos);
        if (record == null) {
            owner.sendSystemMessage(Component.literal("§c[Chest Shop] That chest is not a registered shop!"));
            return 0;
        }

        if (!owner.getUUID().toString().equals(record.ownerUuid)) {
            owner.sendSystemMessage(Component.literal("§c[Chest Shop] Only the shop owner can manage trusted players!"));
            return 0;
        }

        if (targetUuid.toString().equals(record.ownerUuid)) {
            owner.sendSystemMessage(Component.literal("§c[Chest Shop] You are already the owner of this shop!"));
            return 0;
        }

        ChestShopData.addTrusted((ServerLevel) owner.level(), pos, targetUuid, targetName);
        owner.sendSystemMessage(Component.literal("§a✔ Trusted §e" + targetName + " §ain this shop! They can now restock and manage it."));

        if (owner.level().getServer() != null) {
            ServerPlayer targetPlayer = owner.level().getServer().getPlayerList().getPlayer(targetUuid);
            if (targetPlayer != null) {
                targetPlayer.sendSystemMessage(Component.literal("§a✔ You were trusted as a co-manager of §e" + owner.getName().getString() + "§a's shop!"));
            }
        }
        return 1;
    }

    private static int handleUntrust(ServerPlayer owner, UUID targetUuid, String targetName) {
        BlockPos pos = getTargetedChest(owner);
        if (pos == null) {
            owner.sendSystemMessage(Component.literal("§c[Chest Shop] Look directly at your shop chest (within 6 blocks) to untrust someone!"));
            return 0;
        }

        ChestShopData.ShopRecord record = ChestShopData.getRecord(owner.level(), pos);
        if (record == null) {
            owner.sendSystemMessage(Component.literal("§c[Chest Shop] That chest is not a registered shop!"));
            return 0;
        }

        if (!owner.getUUID().toString().equals(record.ownerUuid)) {
            owner.sendSystemMessage(Component.literal("§c[Chest Shop] Only the shop owner can manage trusted players!"));
            return 0;
        }

        if (ChestShopData.removeTrusted((ServerLevel) owner.level(), pos, targetUuid)) {
            owner.sendSystemMessage(Component.literal("§e✔ Removed §f" + targetName + " §efrom this shop's trusted managers."));
            return 1;
        }

        owner.sendSystemMessage(Component.literal("§c[Chest Shop] " + targetName + " is not trusted in this shop."));
        return 0;
    }

    private static int handleListTrusted(ServerPlayer player) {
        BlockPos pos = getTargetedChest(player);
        if (pos == null) {
            player.sendSystemMessage(Component.literal("§c[Chest Shop] Look directly at a shop chest (within 6 blocks) first!"));
            return 0;
        }

        Map<String, String> trusted = ChestShopData.getTrustedNames(player.level(), pos);
        if (trusted.isEmpty()) {
            player.sendSystemMessage(Component.literal("§e[Chest Shop] This shop has no trusted co-managers."));
            return 1;
        }

        player.sendSystemMessage(Component.literal("§6[Chest Shop] Trusted co-managers: §a" + String.join(", ", trusted.values())));
        return 1;
    }

    private static void sendHelp(ServerPlayer player) {
        player.sendSystemMessage(Component.literal("§6============= §e[Simple Chest Shop] §6============="));
        player.sendSystemMessage(Component.literal("§e/shopcreate §7or §e/shop create§f: Open graphical UI to create shop paper."));
        player.sendSystemMessage(Component.literal("§eRight-Click Shop Chest§f: Open graphical Buy Menu to purchase goods."));
        player.sendSystemMessage(Component.literal("§eLeft-Click (Punch) Chest§f: View quick price and stock summary in chat."));
        player.sendSystemMessage(Component.literal("§eOwner/Trusted Right-Click§f: Open chest inventory to restock or collect payment."));
        player.sendSystemMessage(Component.literal("§eOwner/Trusted Sneak + Right-Click§f: View buyer preview GUI."));
        player.sendSystemMessage(Component.literal("§eShift-Click [ CLICK TO BUY ]§f: Bulk-buy as many batches as you can afford."));
        player.sendSystemMessage(Component.literal("§e/shop trust <player>§f: Share management of the shop you're looking at."));
        player.sendSystemMessage(Component.literal("§e/shop untrust <player>§f: Remove a trusted co-manager."));
        player.sendSystemMessage(Component.literal("§e/shop trusted§f: List the trusted co-managers of the shop you're looking at."));
        player.sendSystemMessage(Component.literal("§6================================================="));
    }
}
