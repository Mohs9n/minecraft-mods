package com.simplechestshop;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SimpleChestShopMod implements ModInitializer {
    public static final String MOD_ID = "simplechestshop";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("Initializing Simple Chest Shop mod for Minecraft 26.3...");

        // Load saved shops on server start
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            ChestShopData.load(server.overworld());
            LOGGER.info("Simple Chest Shop data loaded.");
        });

        // Register commands (/shopcreate and /shop)
        net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            com.simplechestshop.command.ShopCommands.register(dispatcher);
        });

        // Handle right-click interaction on chest (Buying or Opening)
        UseBlockCallback.EVENT.register((player, level, hand, hitResult) -> {
            if (hand != InteractionHand.MAIN_HAND) {
                return InteractionResult.PASS;
            }

            BlockPos pos = hitResult.getBlockPos();
            BlockState state = level.getBlockState(pos);

            if (!(state.getBlock() instanceof ChestBlock)) {
                return InteractionResult.PASS;
            }

            if (level.isClientSide()) {
                return InteractionResult.PASS;
            }

            ServerLevel serverLevel = (ServerLevel) level;
            java.util.List<ShopTrade> trades = ChestShopManager.getAllShopTrades(level, pos);

            if (trades.isEmpty()) {
                // If there's no active shop, normal chest behavior
                return InteractionResult.PASS;
            }

            ChestShopData.ShopRecord record = ChestShopData.getRecord(serverLevel, pos);

            // Auto-register owner if not yet registered
            if (record == null) {
                ChestShopData.register(serverLevel, pos, player.getUUID(), player.getName().getString());
                record = ChestShopData.getRecord(serverLevel, pos);
                player.sendSystemMessage(Component.literal("§a[Chest Shop] Shop registered! You are now the owner of this shop."));
            }

            boolean isOwner = player.getUUID().toString().equals(record.ownerUuid);

            if (isOwner) {
                // Owner is opening the chest
                if (player.isShiftKeyDown()) {
                    // Sneak right-click opens the Buyer Preview GUI
                    com.simplechestshop.gui.ShopBuyMenu.open((ServerPlayer) player, level, pos, trades, record.ownerName, record.ownerUuid);
                    return InteractionResult.SUCCESS;
                }
                // Allow owner to open the chest GUI normally to manage stock/earnings
                return InteractionResult.PASS;
            }

            // Customer interaction: Open graphical Buy Menu!
            com.simplechestshop.gui.ShopBuyMenu.open((ServerPlayer) player, level, pos, trades, record.ownerName, record.ownerUuid);
            return InteractionResult.SUCCESS;
        });

        // Handle left-click interaction (punching chest for shop details)
        AttackBlockCallback.EVENT.register((player, level, hand, pos, direction) -> {
            if (level.isClientSide() || hand != InteractionHand.MAIN_HAND) {
                return InteractionResult.PASS;
            }

            BlockState state = level.getBlockState(pos);
            if (!(state.getBlock() instanceof ChestBlock)) {
                return InteractionResult.PASS;
            }

            java.util.List<ShopTrade> trades = ChestShopManager.getAllShopTrades(level, pos);
            if (!trades.isEmpty()) {
                Container container = ChestShopManager.getChestContainer(level, pos);
                ChestShopData.ShopRecord record = ChestShopData.getRecord(level, pos);
                String ownerName = record != null ? record.ownerName : "Unknown";
                ChestShopManager.showShopInfo(player, trades, container, ownerName);
                return InteractionResult.SUCCESS;
            }

            return InteractionResult.PASS;
        });

        // Prevent unauthorized breaking of shop chests
        PlayerBlockBreakEvents.BEFORE.register((level, player, pos, state, blockEntity) -> {
            if (level.isClientSide()) {
                return true;
            }

            if (!(state.getBlock() instanceof ChestBlock)) {
                return true;
            }

            java.util.List<ShopTrade> trades = ChestShopManager.getAllShopTrades(level, pos);
            if (!trades.isEmpty()) {
                ChestShopData.ShopRecord record = ChestShopData.getRecord(level, pos);
                if (record != null && !player.getUUID().toString().equals(record.ownerUuid)) {
                    if (!player.isCreative()) {
                        player.sendSystemMessage(Component.literal("§c[Chest Shop] This shop belongs to " + record.ownerName + "! You cannot break it."));
                        return false;
                    }
                }
                ChestShopData.remove((ServerLevel) level, pos);
            }

            return true;
        });
    }
}
