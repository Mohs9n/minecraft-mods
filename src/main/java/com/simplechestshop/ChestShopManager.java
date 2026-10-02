package com.simplechestshop;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
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
     * Retrieves all valid shop trades from any papers or config items in the chest.
     */
    public static java.util.List<ShopTrade> getAllShopTrades(Level level, BlockPos pos) {
        java.util.List<ShopTrade> trades = new java.util.ArrayList<>();
        Container container = getChestContainer(level, pos);
        if (container == null) {
            return trades;
        }

        for (int i = 0; i < container.getContainerSize(); i++) {
            ItemStack stack = container.getItem(i);
            if (ShopParser.isShopConfigItem(stack)) {
                Component customName = stack.get(DataComponents.CUSTOM_NAME);
                if (customName != null) {
                    java.util.List<ShopTrade> parsed = ShopParser.parseTrades(customName.getString());
                    for (ShopTrade t : parsed) {
                        if (t.isValid()) {
                            trades.add(t);
                        }
                    }
                }
            }
        }
        return trades;
    }

    /**
     * Checks if a block at pos is a chest with a valid shop configuration paper.
     */
    public static ShopTrade getShopTrade(Level level, BlockPos pos) {
        java.util.List<ShopTrade> trades = getAllShopTrades(level, pos);
        return trades.isEmpty() ? null : trades.get(0);
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
     * Shows all shop trade information to a player.
     */
    public static void showShopInfo(Player player, java.util.List<ShopTrade> trades, Container container, String ownerName) {
        if (trades == null || trades.isEmpty()) return;

        player.sendSystemMessage(Component.literal("§6================ §e[Chest Shop] §6================"));
        if (ownerName != null && !ownerName.isBlank()) {
            player.sendSystemMessage(Component.literal("§7Owner: §e" + ownerName));
        }
        player.sendSystemMessage(Component.literal("§7Available Trades / Prices:"));
        for (ShopTrade trade : trades) {
            Item actualSale = getActualSaleItem(container, trade);
            int stock = getStockCount(container, trade);
            Component saleComp = actualSale != null
                    ? Component.literal(trade.getSaleCount() + "x ").append(getItemComponent(actualSale).copy().withStyle(net.minecraft.ChatFormatting.WHITE))
                    : Component.literal("1x [Chest Contents]");
            Component priceComp = Component.literal(trade.getPriceCount() + "x ").append(getItemComponent(trade.getPriceItem()).copy().withStyle(net.minecraft.ChatFormatting.AQUA));
            String stockStr = stock >= trade.getSaleCount() ? "§a" + stock + " in stock" : "§cOut of Stock (" + stock + ")";

            player.sendSystemMessage(Component.literal(" §8• ").append(saleComp)
                    .append(Component.literal(" §7for ")).append(priceComp)
                    .append(Component.literal(" §8(" + stockStr + "§8)")));
        }
        player.sendSystemMessage(Component.literal("§eRight-click chest to open the Buy Menu!"));
        player.sendSystemMessage(Component.literal("§6============================================"));
    }

    /**
     * Shows single shop trade information to a player.
     */
    public static void showShopInfo(Player player, ShopTrade trade, Container container, String ownerName) {
        if (trade == null) return;
        showShopInfo(player, java.util.List.of(trade), container, ownerName);
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

        java.util.List<ShopTrade> trades = getAllShopTrades(level, pos);
        if (trades.isEmpty()) {
            return InteractionResult.PASS;
        }

        ItemStack held = player.getItemInHand(hand);
        ShopTrade matchedTrade = null;
        for (ShopTrade t : trades) {
            if (t.matchesPayment(held)) {
                matchedTrade = t;
                break;
            }
        }

        ChestShopData.ShopRecord record = ChestShopData.getRecord(pos);
        String ownerName = record != null ? record.ownerName : "Unknown";

        // If player is NOT holding matching payment for any trade, show shop info and return
        if (matchedTrade == null) {
            showShopInfo(player, trades, container, ownerName);
            return InteractionResult.SUCCESS;
        }

        ShopTrade trade = matchedTrade;
        int stock = getStockCount(container, trade);
        int requiredSaleCount = trade.getSaleCount();

        if (stock < requiredSaleCount) {
            player.sendSystemMessage(Component.literal("§c[Chest Shop] This trade is currently out of stock!"));
            level.playSound(null, pos, SoundEvents.VILLAGER_NO, SoundSource.BLOCKS, 1.0f, 1.0f);
            return InteractionResult.SUCCESS;
        }

        // Verify that chest has room to store the payment
        ItemStack paymentStack = new ItemStack(trade.getPriceItem(), trade.getPriceCount());
        if (!canStoreItem(container, paymentStack)) {
            player.sendSystemMessage(Component.literal("§c[Chest Shop] Shop chest is full and cannot accept more payments!"));
            return InteractionResult.SUCCESS;
        }

        Item toGiveItem = getActualSaleItem(container, trade);
        if (toGiveItem == null) {
            player.sendSystemMessage(Component.literal("§c[Chest Shop] Shop is out of stock!"));
            return InteractionResult.SUCCESS;
        }

        // 1. Remove sale items from chest
        removeSaleItems(container, toGiveItem, requiredSaleCount);

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
        Component successMsg = Component.literal("§a✔ Purchased " + requiredSaleCount + "x ")
                .append(getItemComponent(toGiveItem).copy().withStyle(net.minecraft.ChatFormatting.GREEN))
                .append(Component.literal("§a for " + trade.getPriceCount() + "x "))
                .append(getItemComponent(trade.getPriceItem()).copy().withStyle(net.minecraft.ChatFormatting.AQUA))
                .append(Component.literal("§a!"));
        player.sendSystemMessage(successMsg);

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

    public static Component getItemComponent(Item item) {
        if (item == null) return Component.literal("Unknown");
        String fallback = getHumanReadableName(item);
        return Component.translatableWithFallback(item.getDescriptionId(), fallback);
    }

    public static String getHumanReadableName(Item item) {
        if (item == null) return "Unknown";
        Identifier id = BuiltInRegistries.ITEM.getKey(item);
        if (id == null) return item.toString();
        String path = id.getPath().replace('_', ' ');
        String[] parts = path.split(" ");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (!part.isEmpty()) {
                sb.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1)).append(" ");
            }
        }
        return sb.toString().trim();
    }

    public static String getItemDisplayName(Item item) {
        return getHumanReadableName(item);
    }
}
