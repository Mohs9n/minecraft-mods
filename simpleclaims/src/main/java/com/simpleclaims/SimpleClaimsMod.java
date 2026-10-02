package com.simpleclaims;

import com.simpleclaims.command.ClaimCommands;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.*;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SimpleClaimsMod implements ModInitializer {
    public static final String MOD_ID = "simpleclaims";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("Initializing Simple Claims mod for Minecraft 26.3...");

        // Load saved claims on server start
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            ClaimManager.load(server.overworld());
            LOGGER.info("Loaded " + ClaimManager.getAllClaims().size() + " land claims.");
        });

        // Register commands
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            ClaimCommands.register(dispatcher);
        });

        // Border crossing notifications every tick
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                ClaimManager.onPlayerTick(player);
            }
        });

        // Clean up disconnected players
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            ClaimManager.onPlayerDisconnect(handler.player.getUUID());
        });

        // Show each player their own sidebar HUD (user, money placeholder, claim status)
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ClaimSidebar.show(handler.player);
        });

        // Wand left-click & block break prevention
        AttackBlockCallback.EVENT.register((player, level, hand, pos, direction) -> {
            if (level.isClientSide()) {
                return InteractionResult.PASS;
            }

            ItemStack held = player.getItemInHand(hand);
            if (hand == InteractionHand.MAIN_HAND && isClaimWand(held)) {
                ClaimManager.setPos1(player, pos);
                return InteractionResult.SUCCESS;
            }

            if (!ClaimManager.canPlayerModify(player, level, pos)) {
                Claim claim = ClaimManager.getClaimAt(level, pos);
                String owner = claim != null ? claim.getOwnerName() : "another player";
                player.sendSystemMessage(Component.literal("§c[SimpleClaims] You cannot break blocks in §e" + owner + "§c's claim!"));
                return InteractionResult.FAIL;
            }

            return InteractionResult.PASS;
        });

        // Wand right-click & block interaction / place prevention
        UseBlockCallback.EVENT.register((player, level, hand, hitResult) -> {
            if (level.isClientSide()) {
                return InteractionResult.PASS;
            }

            BlockPos pos = hitResult.getBlockPos();
            ItemStack held = player.getItemInHand(hand);

            if (hand == InteractionHand.MAIN_HAND && isClaimWand(held)) {
                if (player.isShiftKeyDown() && player instanceof ServerPlayer sp) {
                    com.simpleclaims.gui.ClaimListMenu.open(sp);
                    return InteractionResult.SUCCESS;
                }
                ClaimManager.setPos2(player, pos);
                return InteractionResult.SUCCESS;
            }

            // Check if interacting or placing into a claim
            BlockPos targetPos = pos;
            if (!held.isEmpty()) {
                if (held.getItem() instanceof net.minecraft.world.item.BlockItem) {
                    if (!level.getBlockState(pos).canBeReplaced()) {
                        targetPos = pos.relative(hitResult.getDirection());
                    }
                } else if (held.getItem() == Items.FLINT_AND_STEEL || held.getItem() == Items.LAVA_BUCKET || held.getItem() == Items.WATER_BUCKET) {
                    targetPos = pos.relative(hitResult.getDirection());
                }
            }

            if (!ClaimManager.canPlayerModify(player, level, targetPos) || !ClaimManager.canPlayerModify(player, level, pos)) {
                Claim claim = ClaimManager.getClaimAt(level, targetPos);
                if (claim == null) claim = ClaimManager.getClaimAt(level, pos);
                String owner = claim != null ? claim.getOwnerName() : "another player";
                player.sendSystemMessage(Component.literal("§c[SimpleClaims] You cannot build or interact in §e" + owner + "§c's claim!"));
                return InteractionResult.FAIL;
            }

            return InteractionResult.PASS;
        });

        // Sneak + right-click the wand in open air (no block targeted) also opens the GUI
        UseItemCallback.EVENT.register((player, level, hand) -> {
            if (level.isClientSide() || hand != InteractionHand.MAIN_HAND) {
                return InteractionResult.PASS;
            }

            ItemStack held = player.getItemInHand(hand);
            if (isClaimWand(held) && player.isShiftKeyDown() && player instanceof ServerPlayer sp) {
                com.simpleclaims.gui.ClaimListMenu.open(sp);
                return InteractionResult.SUCCESS;
            }

            return InteractionResult.PASS;
        });

        // Double check on player survival break
        PlayerBlockBreakEvents.BEFORE.register((level, player, pos, state, blockEntity) -> {
            if (level.isClientSide()) {
                return true;
            }

            if (!ClaimManager.canPlayerModify(player, level, pos)) {
                Claim claim = ClaimManager.getClaimAt(level, pos);
                String owner = claim != null ? claim.getOwnerName() : "another player";
                player.sendSystemMessage(Component.literal("§c[SimpleClaims] You cannot break blocks in §e" + owner + "§c's claim!"));
                return false;
            }

            return true;
        });

        // Protect animals, villagers, item frames, armor stands, paintings from attack
        AttackEntityCallback.EVENT.register((player, level, hand, entity, hitResult) -> {
            if (level.isClientSide()) {
                return InteractionResult.PASS;
            }

            if (!ClaimManager.canPlayerDamageEntity(player, entity)) {
                Claim claim = ClaimManager.getClaimAt(level, entity.blockPosition());
                String owner = claim != null ? claim.getOwnerName() : "another player";
                player.sendSystemMessage(Component.literal("§c[SimpleClaims] You cannot harm creatures in §e" + owner + "§c's claim!"));
                return InteractionResult.FAIL;
            }

            return InteractionResult.PASS;
        });

        // Protect item frames and armor stands from interaction (taking items/armor)
        UseEntityCallback.EVENT.register((player, level, hand, entity, hitResult) -> {
            if (level.isClientSide()) {
                return InteractionResult.PASS;
            }

            if (entity instanceof ItemFrame || entity instanceof ArmorStand) {
                if (!ClaimManager.canPlayerModify(player, level, entity.blockPosition())) {
                    Claim claim = ClaimManager.getClaimAt(level, entity.blockPosition());
                    String owner = claim != null ? claim.getOwnerName() : "another player";
                    player.sendSystemMessage(Component.literal("§c[SimpleClaims] You cannot interact with this in §e" + owner + "§c's claim!"));
                    return InteractionResult.FAIL;
                }
            }

            return InteractionResult.PASS;
        });
    }

    private static boolean isClaimWand(ItemStack stack) {
        return ClaimManager.isClaimWand(stack);
    }
}
