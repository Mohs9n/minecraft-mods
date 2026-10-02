package com.simplechestshop;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.UUID;

public class ChestShopManager {

    /**
     * Checks if a block at pos is a chest with a valid shop configuration paper.
     */
    public static ShopTrade getShopTrade(Level level, BlockPos pos) {
        Container container = getChestContainer(level, pos);
        if (container == null) {
            return null;
        }

        for (int i = 0; i < container.getContainerSize(); i++) {
            ItemStack stack = container.getItem(i);
            if (ShopParser.isShopConfigItem(stack)) {
                Component customName = stack.get(DataComponents.CUSTOM_NAME);
                if (customName != null) {
                    ShopTrade trade = ShopParser.parseTrade(customName.getString());
                    if (trade.isValid()) {
                        return trade;
                    }
                }
            }
        }
        return null;
    }

    /**
     * Finds the shop config ItemStack from the container.
     */
    public static ItemStack getShopPaperStack(Container container) {
        if (container == null) return ItemStack.EMPTY;
        for (int i = 0; i < container.getContainerSize(); i++) {
            ItemStack stack = container.getItem(i);
            if (ShopParser.isShopConfigItem(stack)) {
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }

    /**
     * Retrieves the chest container, combining double chests automatically.
     */
    public static Container getChestContainer(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof ChestBlock chestBlock)) {
            return null;
        }
        return ChestBlock.getContainer(chestBlock, state, level, pos, true);
    }

    /**
     * Calculates the amount of stock currently inside the chest for the given trade.
     */
    public static int getStockCount(Container container, ShopTrade trade) {
        if (container == null || trade == null || !trade.isValid()) {
            return 0;
        }

        Item target = trade.getSaleItem();
        int total = 0;
        for (int i = 0; i < container.getContainerSize(); i++) {
            ItemStack stack = container.getItem(i);
            if (ShopParser.isShopConfigItem(stack)) {
                continue; // Do not count or sell the config paper
            }

            if (target != null) {
                if (stack.getItem() == target) {
                    total += stack.getCount();
                }
            } else {
                // If saleItem was not specified in the paper, sell any non-paper items
                if (!stack.isEmpty()) {
                    total += stack.getCount();
                }
            }
        }
        return total;
    }

    /**
     * Shows shop trade information to a player.
     */
    public static void showShopInfo(Player player, ShopTrade trade, Container container, String ownerName) {
        int stock = getStockCount(container, trade);
        String saleName = trade.getSaleItem() != null
                ? trade.getSaleCount() + "x " + getItemDisplayName(trade.getSaleItem())
                : "1x [Chest Contents]";
        String priceName = trade.getPriceCount() + "x " + getItemDisplayName(trade.getPriceItem());

        player.sendSystemMessage(Component.literal("§6================ §e[Chest Shop] §6================"));
        player.sendSystemMessage(Component.literal("§7Item: §f" + saleName));
        player.sendSystemMessage(Component.literal("§7Price: §b" + priceName));
        player.sendSystemMessage(Component.literal("§7Stock: " + (stock >= trade.getSaleCount() ? "§a" + stock : "§cOut of Stock (" + stock + ")")));
        if (ownerName != null && !ownerName.isBlank()) {
            player.sendSystemMessage(Component.literal("§7Owner: §e" + ownerName));
        }
        player.sendSystemMessage(Component.literal("§eRight-click with §b" + priceName + " §eto buy!"));
        player.sendSystemMessage(Component.literal("§6============================================"));
    }

    /**
     * Executes a purchase transaction when a buyer right-clicks the shop chest with payment.
     */
    public static InteractionResult tryPurchase(Player player, Level level, BlockPos pos, InteractionHand hand) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        Container container = getChestContainer(level, pos);
        if (container == null) {
            return InteractionResult.PASS;
        }

        ShopTrade trade = getShopTrade(level, pos);
        if (trade == null || !trade.isValid()) {
            return InteractionResult.PASS;
        }

        ItemStack held = player.getItemInHand(hand);

        // If player is NOT holding the exact payment, show shop info and prevent opening
        if (!trade.matchesPayment(held)) {
            showShopInfo(player, trade, container, null);
            return InteractionResult.SUCCESS;
        }

        int stock = getStockCount(container, trade);
        int requiredSaleCount = trade.getSaleCount();

        if (stock < requiredSaleCount) {
            player.sendSystemMessage(Component.literal("§c[Chest Shop] This shop is currently out of stock!"));
            level.playSound(null, pos, SoundEvents.VILLAGER_NO, SoundSource.BLOCKS, 1.0f, 1.0f);
            return InteractionResult.SUCCESS;
        }

        // Verify that chest has room to store the payment
        ItemStack paymentStack = new ItemStack(trade.getPriceItem(), trade.getPriceCount());
        if (!canStoreItem(container, paymentStack)) {
            player.sendSystemMessage(Component.literal("§c[Chest Shop] Shop chest is full and cannot accept more payments!"));
            return InteractionResult.SUCCESS;
        }

        // Verify which item to take from chest if saleItem is dynamic
        Item toGiveItem = trade.getSaleItem();
        if (toGiveItem == null) {
            for (int i = 0; i < container.getContainerSize(); i++) {
                ItemStack st = container.getItem(i);
                if (!st.isEmpty() && !ShopParser.isShopConfigItem(st)) {
                    toGiveItem = st.getItem();
                    break;
                }
            }
        }

        if (toGiveItem == null) {
            player.sendSystemMessage(Component.literal("§c[Chest Shop] Shop is out of stock!"));
            return InteractionResult.SUCCESS;
        }

        // 1. Remove sale items from chest
        int remainingToRemove = requiredSaleCount;
        for (int i = 0; i < container.getContainerSize(); i++) {
            ItemStack st = container.getItem(i);
            if (ShopParser.isShopConfigItem(st)) continue;

            if (st.getItem() == toGiveItem) {
                int take = Math.min(st.getCount(), remainingToRemove);
                st.shrink(take);
                remainingToRemove -= take;
                if (remainingToRemove <= 0) break;
            }
        }

        // 2. Deposit payment into chest
        storeItem(container, paymentStack);

        // 3. Deduct payment from player hand
        held.shrink(trade.getPriceCount());

        // 4. Give purchased items to buyer
        ItemStack boughtStack = new ItemStack(toGiveItem, requiredSaleCount);
        if (!player.getInventory().add(boughtStack)) {
            player.drop(boughtStack, false, net.minecraft.util.Prediction.SERVER_ONLY);
        }

        container.setChanged();

        // Audio & Visual feedback
        level.playSound(null, pos, SoundEvents.NOTE_BLOCK_CHIME.value(), SoundSource.BLOCKS, 1.0f, 1.5f);
        player.sendSystemMessage(Component.literal("§a✔ Purchased " + requiredSaleCount + "x "
                + getItemDisplayName(toGiveItem) + " for "
                + trade.getPriceCount() + "x " + getItemDisplayName(trade.getPriceItem()) + "!"));

        return InteractionResult.SUCCESS;
    }

    public static boolean canStoreItem(Container container, ItemStack stackToAdd) {
        int left = stackToAdd.getCount();
        for (int i = 0; i < container.getContainerSize(); i++) {
            ItemStack slot = container.getItem(i);
            if (slot.isEmpty()) {
                return true;
            }
            if (ItemStack.isSameItemSameComponents(slot, stackToAdd)) {
                int space = slot.getMaxStackSize() - slot.getCount();
                left -= space;
                if (left <= 0) return true;
            }
        }
        return false;
    }

    public static void storeItem(Container container, ItemStack stackToAdd) {
        for (int i = 0; i < container.getContainerSize(); i++) {
            ItemStack slot = container.getItem(i);
            if (slot.isEmpty()) {
                container.setItem(i, stackToAdd.copy());
                return;
            }
            if (ItemStack.isSameItemSameComponents(slot, stackToAdd)) {
                int space = slot.getMaxStackSize() - slot.getCount();
                int add = Math.min(space, stackToAdd.getCount());
                slot.grow(add);
                stackToAdd.shrink(add);
                if (stackToAdd.isEmpty()) return;
            }
        }
    }

    public static int countPlayerItem(Player player, Item item) {
        if (player == null || item == null) return 0;
        int total = 0;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack st = player.getInventory().getItem(i);
            if (!st.isEmpty() && st.getItem() == item) {
                total += st.getCount();
            }
        }
        return total;
    }

    public static boolean deductPlayerItem(Player player, Item item, int count) {
        if (countPlayerItem(player, item) < count) {
            return false;
        }
        int remaining = count;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack st = player.getInventory().getItem(i);
            if (!st.isEmpty() && st.getItem() == item) {
                int take = Math.min(st.getCount(), remaining);
                st.shrink(take);
                remaining -= take;
                if (remaining <= 0) break;
            }
        }
        player.getInventory().setChanged();
        return true;
    }

    public static Item getActualSaleItem(Container container, ShopTrade trade) {
        if (trade == null) return null;
        if (trade.getSaleItem() != null) return trade.getSaleItem();
        if (container == null) return null;

        for (int i = 0; i < container.getContainerSize(); i++) {
            ItemStack st = container.getItem(i);
            if (!st.isEmpty() && !ShopParser.isShopConfigItem(st)) {
                return st.getItem();
            }
        }
        return null;
    }

    public static boolean removeSaleItems(Container container, Item item, int count) {
        if (container == null || item == null || count <= 0) return false;
        int remaining = count;
        for (int i = 0; i < container.getContainerSize(); i++) {
            ItemStack st = container.getItem(i);
            if (ShopParser.isShopConfigItem(st)) continue;

            if (st.getItem() == item) {
                int take = Math.min(st.getCount(), remaining);
                st.shrink(take);
                remaining -= take;
                if (remaining <= 0) {
                    container.setChanged();
                    return true;
                }
            }
        }
        container.setChanged();
        return remaining <= 0;
    }

    public static String getItemDisplayName(Item item) {
        if (item == null) return "Unknown";
        return item.getName(ItemStack.EMPTY).getString();
    }
}
