package com.simplechestshop.gui;

import com.simplechestshop.ChestShopManager;
import com.simplechestshop.ShopTrade;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Prediction;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ShopBuyMenu extends ChestMenu {

    private static final int MAX_BULK_BATCHES = 64;

    private final Level level;
    private final BlockPos chestPos;
    private final List<ShopTrade> trades;
    private final String ownerName;
    private final String ownerUuid;

    public ShopBuyMenu(int containerId, Inventory playerInventory, Level level, BlockPos chestPos, List<ShopTrade> trades, String ownerName, String ownerUuid) {
        super(trades != null && trades.size() > 3 ? MenuType.GENERIC_9x6 : MenuType.GENERIC_9x3,
                containerId,
                playerInventory,
                new SimpleContainer(trades != null && trades.size() > 3 ? 54 : 27),
                trades != null && trades.size() > 3 ? 6 : 3);
        this.level = level;
        this.chestPos = chestPos;
        this.trades = (trades != null) ? trades : List.of();
        this.ownerName = ownerName;
        this.ownerUuid = ownerUuid;

        initBorders();
        updateGui(playerInventory.player);
    }

    public static void open(ServerPlayer player, Level level, BlockPos pos, List<ShopTrade> trades, String ownerName, String ownerUuid) {
        player.openMenu(new SimpleMenuProvider(
                (containerId, playerInv, p) -> new ShopBuyMenu(containerId, playerInv, level, pos, trades, ownerName, ownerUuid),
                Component.literal("§6Chest Shop §8| §e" + (ownerName != null ? ownerName : "Shop"))
        ));
    }

    public static void open(ServerPlayer player, Level level, BlockPos pos, ShopTrade trade, String ownerName, String ownerUuid) {
        open(player, level, pos, trade != null ? List.of(trade) : List.of(), ownerName, ownerUuid);
    }

    private void initBorders() {
        ItemStack border = ShopCreationMenu.createGuiItem(Items.STAINED_GLASS_PANE.gray(), Component.literal("§r"), null);
        for (int i = 0; i < getContainer().getContainerSize(); i++) {
            getContainer().setItem(i, border.copy());
        }
    }

    public void updateGui(Player player) {
        initBorders();

        Container chestContainer = ChestShopManager.getChestContainer(level, chestPos);
        if (chestContainer == null || trades.isEmpty()) {
            ItemStack emptyItem = ShopCreationMenu.createGuiItem(
                    Items.BARRIER,
                    Component.literal("§cNo Trades Available"),
                    List.of(Component.literal("§7This chest has no active shop trades."))
            );
            int midSlot = getContainer().getContainerSize() / 2;
            getContainer().setItem(midSlot, emptyItem);
            return;
        }

        int maxTrades = (getContainer().getContainerSize() == 54) ? 6 : 3;
        int countToRender = Math.min(trades.size(), maxTrades);

        for (int i = 0; i < countToRender; i++) {
            ShopTrade trade = trades.get(i);
            int row = (trades.size() == 1) ? 1 : i;

            int productSlot = row * 9 + 2;
            int arrow1Slot  = row * 9 + 3;
            int priceSlot   = row * 9 + 4;
            int arrow2Slot  = row * 9 + 5;
            int buySlot     = row * 9 + 6;

            int stock = ChestShopManager.getStockCount(chestContainer, trade);
            Item actualSaleItem = ChestShopManager.getActualSaleItem(chestContainer, trade);
            int buyerBalance = ChestShopManager.countPlayerItem(player, trade.getPriceItem());

            // Product Display (Slot 2 in row)
            if (actualSaleItem != null) {
                ItemStack product = new ItemStack(actualSaleItem, Math.min(trade.getSaleCount(), actualSaleItem.getDefaultMaxStackSize()));
                List<Component> lore = new ArrayList<>();
                lore.add(Component.literal("§7§m------------------------"));
                lore.add(Component.literal("§7Product: §f" + trade.getSaleCount() + "x ").append(ChestShopManager.getItemComponent(actualSaleItem).copy().withStyle(net.minecraft.ChatFormatting.WHITE)));
                lore.add(Component.literal("§7Chest Stock: " + (stock >= trade.getSaleCount() ? "§a" + stock + " available" : "§cOut of Stock (" + stock + ")")));
                if (trades.size() > 1) {
                    lore.add(Component.literal("§6Price Option: §e#" + (i + 1)));
                }
                lore.add(Component.literal("§7Shop Owner: §e" + (ownerName != null ? ownerName : "Unknown")));
                lore.add(Component.literal("§7§m------------------------"));
                product.set(DataComponents.LORE, new ItemLore(lore));
                getContainer().setItem(productSlot, product);
            } else {
                ItemStack emptyProd = ShopCreationMenu.createGuiItem(
                        Items.BARRIER,
                        Component.literal("§cOut of Stock"),
                        List.of(Component.literal("§7No items currently in chest to sell."))
                );
                getContainer().setItem(productSlot, emptyProd);
            }

            // Arrow 1: "FOR"
            ItemStack arrow1 = ShopCreationMenu.createGuiItem(
                    Items.STAINED_GLASS_PANE.lightBlue(),
                    Component.literal("§b➡ FOR ➡"),
                    List.of(Component.literal("§7Cost requirement:"))
            );
            getContainer().setItem(arrow1Slot, arrow1);

            // Price Display (Slot 4 in row)
            ItemStack price = new ItemStack(trade.getPriceItem(), Math.min(trade.getPriceCount(), trade.getPriceItem().getDefaultMaxStackSize()));
            List<Component> priceLore = new ArrayList<>();
            priceLore.add(Component.literal("§7§m------------------------"));
            priceLore.add(Component.literal("§7Price: §b" + trade.getPriceCount() + "x ").append(ChestShopManager.getItemComponent(trade.getPriceItem()).copy().withStyle(net.minecraft.ChatFormatting.AQUA)));
            priceLore.add(Component.literal("§7Your Balance: " + (buyerBalance >= trade.getPriceCount() ? "§a" + buyerBalance + " in inventory" : "§c" + buyerBalance + " (Need " + (trade.getPriceCount() - buyerBalance) + " more)")));
            priceLore.add(Component.literal("§7§m------------------------"));
            price.set(DataComponents.LORE, new ItemLore(priceLore));
            getContainer().setItem(priceSlot, price);

            // Arrow 2: "BUY"
            ItemStack arrow2 = ShopCreationMenu.createGuiItem(
                    Items.STAINED_GLASS_PANE.lightBlue(),
                    Component.literal("§a➡ BUY ➡"),
                    List.of(Component.literal("§7Click button on right to buy"))
            );
            getContainer().setItem(arrow2Slot, arrow2);

            // Buy Button (Slot 6 in row)
            if (stock < trade.getSaleCount() || actualSaleItem == null) {
                ItemStack outOfStockBtn = ShopCreationMenu.createGuiItem(
                        Items.STAINED_GLASS_PANE.red(),
                        Component.literal("§c§l[ OUT OF STOCK ]"),
                        List.of(
                                Component.literal("§7The chest does not have enough stock!"),
                                Component.literal("§8Stock: " + stock + " / " + trade.getSaleCount())
                        )
                );
                getContainer().setItem(buySlot, outOfStockBtn);
            } else if (buyerBalance < trade.getPriceCount()) {
                ItemStack cannotAffordBtn = ShopCreationMenu.createGuiItem(
                        Items.STAINED_GLASS_PANE.yellow(),
                        Component.literal("§e§l[ CANNOT AFFORD ]"),
                        List.of(
                                Component.literal("§7You need §b" + trade.getPriceCount() + "x ").append(ChestShopManager.getItemComponent(trade.getPriceItem())),
                                Component.literal("§7You currently have: §e" + buyerBalance + "x")
                        )
                );
                getContainer().setItem(buySlot, cannotAffordBtn);
            } else {
                ItemStack buyBtn = ShopCreationMenu.createGuiItem(
                        Items.STAINED_GLASS_PANE.lime(),
                        Component.literal("§a§l[ CLICK TO BUY ]"),
                        List.of(
                                Component.literal("§7Cost: §b" + trade.getPriceCount() + "x ").append(ChestShopManager.getItemComponent(trade.getPriceItem())),
                                Component.literal("§7Receive: §f" + trade.getSaleCount() + "x ").append(ChestShopManager.getItemComponent(actualSaleItem)),
                                Component.literal("§eClick here to purchase 1 batch!"),
                                Component.literal("§6Shift-click to bulk buy §7(up to " + MAX_BULK_BATCHES + " batches)!")
                        )
                );
                getContainer().setItem(buySlot, buyBtn);
            }
        }
    }

    @Override
    public void clicked(int slotIndex, int button, ContainerInput input, Player player) {
        int containerSize = getContainer().getContainerSize();

        // Clicks inside the shop menu
        if (slotIndex >= 0 && slotIndex < containerSize) {
            int maxTrades = (containerSize == 54) ? 6 : 3;
            int count = Math.min(trades.size(), maxTrades);

            for (int i = 0; i < count; i++) {
                int row = (trades.size() == 1) ? 1 : i;
                int buySlot = row * 9 + 6;

                if (slotIndex == buySlot) {
                    if (input == ContainerInput.QUICK_MOVE) {
                        handleBulkPurchase(player, trades.get(i));
                    } else {
                        handlePurchase(player, trades.get(i));
                    }
                    break;
                }
            }
            sendAllDataToRemote();
            return;
        }

        // Clicks in player inventory
        if (slotIndex >= containerSize) {
            if (input == ContainerInput.QUICK_MOVE) {
                // Prevent shift-clicking items into the GUI
                sendAllDataToRemote();
                return;
            }
            super.clicked(slotIndex, button, input, player);
            updateGui(player);
            sendAllDataToRemote();
        }
    }

    private void handlePurchase(Player player, ShopTrade trade) {
        Container chestContainer = ChestShopManager.getChestContainer(level, chestPos);
        if (chestContainer == null) {
            if (player instanceof ServerPlayer sp) {
                sp.closeContainer();
            }
            return;
        }

        int stock = ChestShopManager.getStockCount(chestContainer, trade);
        if (stock < trade.getSaleCount()) {
            player.sendSystemMessage(Component.literal("§c[Chest Shop] This trade is out of stock!"));
            level.playSound(null, player.blockPosition(), SoundEvents.VILLAGER_NO, SoundSource.PLAYERS, 1.0f, 1.0f);
            updateGui(player);
            return;
        }

        int buyerBalance = ChestShopManager.countPlayerItem(player, trade.getPriceItem());
        if (buyerBalance < trade.getPriceCount()) {
            Component needMsg = Component.literal("§c[Chest Shop] You need " + trade.getPriceCount() + "x ")
                    .append(ChestShopManager.getItemComponent(trade.getPriceItem()))
                    .append(Component.literal(" to buy this!"));
            player.sendSystemMessage(needMsg);
            level.playSound(null, player.blockPosition(), SoundEvents.VILLAGER_NO, SoundSource.PLAYERS, 1.0f, 1.0f);
            updateGui(player);
            return;
        }

        ItemStack paymentStack = new ItemStack(trade.getPriceItem(), trade.getPriceCount());
        if (!ChestShopManager.canStoreItem(chestContainer, paymentStack)) {
            player.sendSystemMessage(Component.literal("§c[Chest Shop] The shop chest is full and cannot accept more payments!"));
            level.playSound(null, player.blockPosition(), SoundEvents.VILLAGER_NO, SoundSource.PLAYERS, 1.0f, 1.0f);
            updateGui(player);
            return;
        }

        Item actualSaleItem = ChestShopManager.getActualSaleItem(chestContainer, trade);
        if (actualSaleItem == null) {
            player.sendSystemMessage(Component.literal("§c[Chest Shop] Out of stock!"));
            updateGui(player);
            return;
        }

        // Execute transaction
        // 1. Deduct payment from buyer
        if (!ChestShopManager.deductPlayerItem(player, trade.getPriceItem(), trade.getPriceCount())) {
            updateGui(player);
            return;
        }

        // 2. Deposit payment to chest
        ChestShopManager.storeItem(chestContainer, paymentStack);

        // 3. Remove sale items from chest
        ChestShopManager.removeSaleItems(chestContainer, actualSaleItem, trade.getSaleCount());

        // 4. Give items to buyer
        ItemStack boughtStack = new ItemStack(actualSaleItem, trade.getSaleCount());
        if (!player.getInventory().add(boughtStack)) {
            player.drop(boughtStack, false, Prediction.SERVER_ONLY);
        }

        // Feedback sound
        level.playSound(null, player.blockPosition(), SoundEvents.NOTE_BLOCK_CHIME.value(), SoundSource.PLAYERS, 1.0f, 1.5f);
        Component purchaseMsg = Component.literal("§a✔ Purchased " + trade.getSaleCount() + "x ")
                .append(ChestShopManager.getItemComponent(actualSaleItem).copy().withStyle(net.minecraft.ChatFormatting.GREEN))
                .append(Component.literal("§a for " + trade.getPriceCount() + "x "))
                .append(ChestShopManager.getItemComponent(trade.getPriceItem()).copy().withStyle(net.minecraft.ChatFormatting.AQUA))
                .append(Component.literal("§a!"));
        player.sendSystemMessage(purchaseMsg);

        // Notify owner if online
        notifyOwner(player, trade, actualSaleItem, trade.getSaleCount(), trade.getPriceCount());

        // Update GUI display
        updateGui(player);
    }

    /**
     * Bulk-buy: repeats the single-batch transaction (same checks, same methods) up to
     * MAX_BULK_BATCHES times or until stock, balance, or chest storage space runs out,
     * then reports one combined summary instead of spamming a message per batch.
     */
    private void handleBulkPurchase(Player player, ShopTrade trade) {
        Container chestContainer = ChestShopManager.getChestContainer(level, chestPos);
        if (chestContainer == null) {
            if (player instanceof ServerPlayer sp) {
                sp.closeContainer();
            }
            return;
        }

        int batchesBought = 0;
        int totalSale = 0;
        int totalPrice = 0;
        Item actualSaleItem = null;

        for (int i = 0; i < MAX_BULK_BATCHES; i++) {
            int stock = ChestShopManager.getStockCount(chestContainer, trade);
            if (stock < trade.getSaleCount()) break;

            int buyerBalance = ChestShopManager.countPlayerItem(player, trade.getPriceItem());
            if (buyerBalance < trade.getPriceCount()) break;

            ItemStack paymentStack = new ItemStack(trade.getPriceItem(), trade.getPriceCount());
            if (!ChestShopManager.canStoreItem(chestContainer, paymentStack)) break;

            Item saleItem = ChestShopManager.getActualSaleItem(chestContainer, trade);
            if (saleItem == null) break;

            if (!ChestShopManager.deductPlayerItem(player, trade.getPriceItem(), trade.getPriceCount())) break;

            ChestShopManager.storeItem(chestContainer, paymentStack);
            ChestShopManager.removeSaleItems(chestContainer, saleItem, trade.getSaleCount());

            ItemStack boughtStack = new ItemStack(saleItem, trade.getSaleCount());
            if (!player.getInventory().add(boughtStack)) {
                player.drop(boughtStack, false, Prediction.SERVER_ONLY);
            }

            batchesBought++;
            totalSale += trade.getSaleCount();
            totalPrice += trade.getPriceCount();
            actualSaleItem = saleItem;
        }

        if (batchesBought == 0) {
            // Nothing could be bought at all - fall back to the single-purchase flow so the
            // player gets a precise reason (out of stock / can't afford / chest full).
            handlePurchase(player, trade);
            return;
        }

        level.playSound(null, player.blockPosition(), SoundEvents.NOTE_BLOCK_CHIME.value(), SoundSource.PLAYERS, 1.0f, 1.5f);
        Component bulkMsg = Component.literal("§a✔ Bulk purchased §f" + batchesBought + " batch" + (batchesBought == 1 ? "" : "es") + "§a: " + totalSale + "x ")
                .append(ChestShopManager.getItemComponent(actualSaleItem).copy().withStyle(net.minecraft.ChatFormatting.GREEN))
                .append(Component.literal("§a for " + totalPrice + "x "))
                .append(ChestShopManager.getItemComponent(trade.getPriceItem()).copy().withStyle(net.minecraft.ChatFormatting.AQUA))
                .append(Component.literal("§a!"));
        player.sendSystemMessage(bulkMsg);

        notifyOwner(player, trade, actualSaleItem, totalSale, totalPrice);
        updateGui(player);
    }

    private void notifyOwner(Player buyer, ShopTrade trade, Item saleItem, int saleCount, int priceCount) {
        if (ownerUuid == null || ownerUuid.isBlank() || level.getServer() == null) return;
        try {
            UUID uuid = UUID.fromString(ownerUuid);
            ServerPlayer ownerPlayer = level.getServer().getPlayerList().getPlayer(uuid);
            if (ownerPlayer != null && ownerPlayer.isAlive()) {
                Component notifyMsg = Component.literal("§e[Shop] " + buyer.getName().getString() + " bought " + saleCount + "x ")
                        .append(ChestShopManager.getItemComponent(saleItem).copy().withStyle(net.minecraft.ChatFormatting.YELLOW))
                        .append(Component.literal(" for " + priceCount + "x "))
                        .append(ChestShopManager.getItemComponent(trade.getPriceItem()).copy().withStyle(net.minecraft.ChatFormatting.AQUA))
                        .append(Component.literal(" at (" + chestPos.getX() + ", " + chestPos.getY() + ", " + chestPos.getZ() + ")!"));
                ownerPlayer.sendSystemMessage(notifyMsg);
            }
        } catch (IllegalArgumentException ignored) {}
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slotIndex) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        BlockState state = level.getBlockState(chestPos);
        if (!(state.getBlock() instanceof ChestBlock)) {
            return false;
        }
        return player.distanceToSqr(chestPos.getX() + 0.5D, chestPos.getY() + 0.5D, chestPos.getZ() + 0.5D) <= 64.0D;
    }
}
