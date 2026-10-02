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
    public static final int PRODUCT_DISPLAY_SLOT = 11;
    public static final int PRICE_DISPLAY_SLOT = 13;
    public static final int BUY_BUTTON_SLOT = 15;

    private final Level level;
    private final BlockPos chestPos;
    private final ShopTrade trade;
    private final String ownerName;
    private final String ownerUuid;

    public ShopBuyMenu(int containerId, Inventory playerInventory, Level level, BlockPos chestPos, ShopTrade trade, String ownerName, String ownerUuid) {
        super(MenuType.GENERIC_9x3, containerId, playerInventory, new SimpleContainer(27), 3);
        this.level = level;
        this.chestPos = chestPos;
        this.trade = trade;
        this.ownerName = ownerName;
        this.ownerUuid = ownerUuid;

        initBorders();
        updateGui(playerInventory.player);
    }

    public static void open(ServerPlayer player, Level level, BlockPos pos, ShopTrade trade, String ownerName, String ownerUuid) {
        player.openMenu(new SimpleMenuProvider(
                (containerId, playerInv, p) -> new ShopBuyMenu(containerId, playerInv, level, pos, trade, ownerName, ownerUuid),
                Component.literal("§6Chest Shop §8| §e" + (ownerName != null ? ownerName : "Shop"))
        ));
    }

    private void initBorders() {
        ItemStack border = ShopCreationMenu.createGuiItem(Items.STAINED_GLASS_PANE.gray(), Component.literal("§r"), null);
        for (int i = 0; i < 27; i++) {
            if (i != PRODUCT_DISPLAY_SLOT && i != PRICE_DISPLAY_SLOT && i != BUY_BUTTON_SLOT) {
                getContainer().setItem(i, border);
            }
        }
    }

    public void updateGui(Player player) {
        Container chestContainer = ChestShopManager.getChestContainer(level, chestPos);
        if (chestContainer == null) return;

        int stock = ChestShopManager.getStockCount(chestContainer, trade);
        Item actualSaleItem = ChestShopManager.getActualSaleItem(chestContainer, trade);
        int buyerBalance = ChestShopManager.countPlayerItem(player, trade.getPriceItem());

        // Slot 11: Product Display
        if (actualSaleItem != null) {
            ItemStack product = new ItemStack(actualSaleItem, Math.min(trade.getSaleCount(), actualSaleItem.getDefaultMaxStackSize()));
            List<Component> lore = new ArrayList<>();
            lore.add(Component.literal("§7§m------------------------"));
            lore.add(Component.literal("§7Product: §f" + trade.getSaleCount() + "x " + ChestShopManager.getItemDisplayName(actualSaleItem)));
            lore.add(Component.literal("§7Chest Stock: " + (stock >= trade.getSaleCount() ? "§a" + stock + " available" : "§cOut of Stock (" + stock + ")")));
            lore.add(Component.literal("§7Shop Owner: §e" + (ownerName != null ? ownerName : "Unknown")));
            lore.add(Component.literal("§7§m------------------------"));
            product.set(DataComponents.LORE, new ItemLore(lore));
            getContainer().setItem(PRODUCT_DISPLAY_SLOT, product);
        } else {
            ItemStack emptyProd = ShopCreationMenu.createGuiItem(
                    Items.BARRIER,
                    Component.literal("§cOut of Stock"),
                    List.of(Component.literal("§7No items currently in chest to sell."))
            );
            getContainer().setItem(PRODUCT_DISPLAY_SLOT, emptyProd);
        }

        // Slot 13: Price Display
        ItemStack price = new ItemStack(trade.getPriceItem(), Math.min(trade.getPriceCount(), trade.getPriceItem().getDefaultMaxStackSize()));
        List<Component> priceLore = new ArrayList<>();
        priceLore.add(Component.literal("§7§m------------------------"));
        priceLore.add(Component.literal("§7Price: §b" + trade.getPriceCount() + "x " + ChestShopManager.getItemDisplayName(trade.getPriceItem())));
        priceLore.add(Component.literal("§7Your Balance: " + (buyerBalance >= trade.getPriceCount() ? "§a" + buyerBalance + " in inventory" : "§c" + buyerBalance + " (Need " + (trade.getPriceCount() - buyerBalance) + " more)")));
        priceLore.add(Component.literal("§7§m------------------------"));
        price.set(DataComponents.LORE, new ItemLore(priceLore));
        getContainer().setItem(PRICE_DISPLAY_SLOT, price);

        // Slot 15: Buy Button
        if (stock < trade.getSaleCount() || actualSaleItem == null) {
            ItemStack outOfStockBtn = ShopCreationMenu.createGuiItem(
                    Items.STAINED_GLASS_PANE.red(),
                    Component.literal("§c§l[ OUT OF STOCK ]"),
                    List.of(
                            Component.literal("§7The chest does not have enough stock!"),
                            Component.literal("§8Current stock: " + stock + " / " + trade.getSaleCount())
                    )
            );
            getContainer().setItem(BUY_BUTTON_SLOT, outOfStockBtn);
        } else if (buyerBalance < trade.getPriceCount()) {
            ItemStack cannotAffordBtn = ShopCreationMenu.createGuiItem(
                    Items.STAINED_GLASS_PANE.yellow(),
                    Component.literal("§e§l[ CANNOT AFFORD ]"),
                    List.of(
                            Component.literal("§7You need §b" + trade.getPriceCount() + "x " + ChestShopManager.getItemDisplayName(trade.getPriceItem())),
                            Component.literal("§7You currently have: §e" + buyerBalance + "x")
                    )
            );
            getContainer().setItem(BUY_BUTTON_SLOT, cannotAffordBtn);
        } else {
            ItemStack buyBtn = ShopCreationMenu.createGuiItem(
                    Items.STAINED_GLASS_PANE.lime(),
                    Component.literal("§a§l[ CLICK TO BUY ]"),
                    List.of(
                            Component.literal("§7Cost: §b" + trade.getPriceCount() + "x " + ChestShopManager.getItemDisplayName(trade.getPriceItem())),
                            Component.literal("§7Receive: §f" + trade.getSaleCount() + "x " + ChestShopManager.getItemDisplayName(actualSaleItem)),
                            Component.literal("§eClick here to purchase 1 batch!")
                    )
            );
            getContainer().setItem(BUY_BUTTON_SLOT, buyBtn);
        }
    }

    @Override
    public void clicked(int slotIndex, int button, ContainerInput input, Player player) {
        // Clicks inside the shop menu (top 27 slots)
        if (slotIndex >= 0 && slotIndex < 27) {
            if (slotIndex == BUY_BUTTON_SLOT) {
                handlePurchase(player);
            }
            sendAllDataToRemote();
            return;
        }

        // Clicks in player inventory
        if (slotIndex >= 27) {
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

    private void handlePurchase(Player player) {
        Container chestContainer = ChestShopManager.getChestContainer(level, chestPos);
        if (chestContainer == null) {
            if (player instanceof ServerPlayer sp) {
                sp.closeContainer();
            }
            return;
        }

        int stock = ChestShopManager.getStockCount(chestContainer, trade);
        if (stock < trade.getSaleCount()) {
            player.sendSystemMessage(Component.literal("§c[Chest Shop] This shop is out of stock!"));
            level.playSound(null, player.blockPosition(), SoundEvents.VILLAGER_NO, SoundSource.PLAYERS, 1.0f, 1.0f);
            updateGui(player);
            return;
        }

        int buyerBalance = ChestShopManager.countPlayerItem(player, trade.getPriceItem());
        if (buyerBalance < trade.getPriceCount()) {
            player.sendSystemMessage(Component.literal("§c[Chest Shop] You need " + trade.getPriceCount() + "x " + ChestShopManager.getItemDisplayName(trade.getPriceItem()) + " to buy this!"));
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
        player.sendSystemMessage(Component.literal("§a✔ Purchased " + trade.getSaleCount() + "x "
                + ChestShopManager.getItemDisplayName(actualSaleItem) + " for "
                + trade.getPriceCount() + "x " + ChestShopManager.getItemDisplayName(trade.getPriceItem()) + "!"));

        // Notify owner if online
        notifyOwner(player, actualSaleItem);

        // Update GUI display
        updateGui(player);
    }

    private void notifyOwner(Player buyer, Item saleItem) {
        if (ownerUuid == null || ownerUuid.isBlank() || level.getServer() == null) return;
        try {
            UUID uuid = UUID.fromString(ownerUuid);
            ServerPlayer ownerPlayer = level.getServer().getPlayerList().getPlayer(uuid);
            if (ownerPlayer != null && ownerPlayer.isAlive()) {
                ownerPlayer.sendSystemMessage(Component.literal("§e[Shop] " + buyer.getName().getString()
                        + " bought " + trade.getSaleCount() + "x " + ChestShopManager.getItemDisplayName(saleItem)
                        + " for " + trade.getPriceCount() + "x " + ChestShopManager.getItemDisplayName(trade.getPriceItem())
                        + " at (" + chestPos.getX() + ", " + chestPos.getY() + ", " + chestPos.getZ() + ")!"));
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
