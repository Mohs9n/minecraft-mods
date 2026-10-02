package com.simplechestshop.gui;

import com.simplechestshop.ShopParser;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
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
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemLore;

import java.util.ArrayList;
import java.util.List;

public class ShopCreationMenu extends ChestMenu {
    public static final int PRICE_SLOT = 11;
    public static final int SALE_SLOT = 13;
    public static final int CREATE_BUTTON_SLOT = 15;
    public static final int PRICE_INFO_SLOT = 10;
    public static final int SALE_INFO_SLOT = 12;

    public ShopCreationMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, new SimpleContainer(27));
    }

    public ShopCreationMenu(int containerId, Inventory playerInventory, Container container) {
        super(MenuType.GENERIC_9x3, containerId, playerInventory, container, 3);
        setupGui();
    }

    public static void open(ServerPlayer player) {
        player.openMenu(new SimpleMenuProvider(
                (containerId, playerInv, p) -> new ShopCreationMenu(containerId, playerInv),
                Component.literal("§6Create Shop Paper")
        ));
    }

    private void setupGui() {
        ItemStack border = createGuiItem(Items.STAINED_GLASS_PANE.gray(), Component.literal("§r"), null);
        for (int i = 0; i < 27; i++) {
            if (i == PRICE_SLOT || i == SALE_SLOT) {
                continue;
            }
            getContainer().setItem(i, border);
        }

        // Info icons
        ItemStack priceInfo = createGuiItem(
                Items.GOLD_INGOT,
                Component.literal("§6§lPrice Slot §7→"),
                List.of(
                        Component.literal("§7Put the currency/price you want to charge"),
                        Component.literal("§7into the slot on the right."),
                        Component.literal("§eExample: 1x Diamond or 2x Emerald")
                )
        );
        getContainer().setItem(PRICE_INFO_SLOT, priceInfo);

        ItemStack saleInfo = createGuiItem(
                Items.CHEST,
                Component.literal("§b§lSale Slot §7→"),
                List.of(
                        Component.literal("§7Put the item you want to sell into"),
                        Component.literal("§7the slot on the right (optional)."),
                        Component.literal("§8If empty, shop will sell any chest contents.")
                )
        );
        getContainer().setItem(SALE_INFO_SLOT, saleInfo);

        // Create Button
        ItemStack createBtn = createGuiItem(
                Items.WRITABLE_BOOK,
                Component.literal("§a§l[ Create Shop Paper ]"),
                List.of(
                        Component.literal("§7Click here to generate your shop paper!"),
                        Component.literal("§7Your sample items will be safely returned."),
                        Component.literal("§eRequires at least a Price item.")
                )
        );
        getContainer().setItem(CREATE_BUTTON_SLOT, createBtn);
    }

    @Override
    public void clicked(int slotIndex, int button, ContainerInput input, Player player) {
        // Clicks inside the top 27 slots
        if (slotIndex >= 0 && slotIndex < 27) {
            if (slotIndex == CREATE_BUTTON_SLOT) {
                handleCreatePaper(player);
                sendAllDataToRemote();
                return;
            }

            // Only slot 11 and 13 are editable by player
            if (slotIndex == PRICE_SLOT || slotIndex == SALE_SLOT) {
                super.clicked(slotIndex, button, input, player);
                return;
            }

            // Cancel any clicks on border/info items
            sendAllDataToRemote();
            return;
        }

        // Clicks in player inventory (slotIndex >= 27)
        if (slotIndex >= 27) {
            if (input == ContainerInput.QUICK_MOVE) {
                // Shift-click handling
                Slot slot = getSlot(slotIndex);
                if (slot != null && slot.hasItem()) {
                    ItemStack moving = slot.getItem();
                    // Try slot 11 first if empty
                    if (getContainer().getItem(PRICE_SLOT).isEmpty()) {
                        getContainer().setItem(PRICE_SLOT, moving.copy());
                        slot.set(ItemStack.EMPTY);
                    } else if (getContainer().getItem(SALE_SLOT).isEmpty()) {
                        getContainer().setItem(SALE_SLOT, moving.copy());
                        slot.set(ItemStack.EMPTY);
                    }
                    broadcastChanges();
                }
                sendAllDataToRemote();
                return;
            }
            super.clicked(slotIndex, button, input, player);
        }
    }

    private void handleCreatePaper(Player player) {
        ItemStack priceStack = getContainer().getItem(PRICE_SLOT);
        ItemStack saleStack = getContainer().getItem(SALE_SLOT);

        if (priceStack.isEmpty()) {
            player.sendSystemMessage(Component.literal("§c[Shop Creator] Please place a price item in the Price slot (left)!"));
            if (player.level() != null) {
                player.level().playSound(null, player.blockPosition(), SoundEvents.VILLAGER_NO, SoundSource.PLAYERS, 1.0f, 1.0f);
            }
            return;
        }

        Identifier priceId = BuiltInRegistries.ITEM.getKey(priceStack.getItem());
        int priceCount = priceStack.getCount();
        String priceName = priceId.getPath();

        String generatedTradeText;
        if (!saleStack.isEmpty()) {
            Identifier saleId = BuiltInRegistries.ITEM.getKey(saleStack.getItem());
            int saleCount = saleStack.getCount();
            String saleName = saleId.getPath();
            generatedTradeText = priceCount + " " + priceName + " -> " + saleCount + " " + saleName;
        } else {
            generatedTradeText = priceCount + " " + priceName;
        }

        // Create the paper
        ItemStack paper = new ItemStack(Items.PAPER);
        paper.set(DataComponents.CUSTOM_NAME, Component.literal(generatedTradeText));

        List<Component> lore = new ArrayList<>();
        lore.add(Component.literal("§7§m------------------------"));
        lore.add(Component.literal("§6Chest Shop Config Paper"));
        lore.add(Component.literal("§7Price: §b" + priceCount + "x " + priceStack.getHoverName().getString()));
        if (!saleStack.isEmpty()) {
            lore.add(Component.literal("§7Selling: §f" + saleStack.getCount() + "x " + saleStack.getHoverName().getString()));
        } else {
            lore.add(Component.literal("§7Selling: §f[Any Chest Items]"));
        }
        lore.add(Component.literal("§ePlace this in a chest to activate!"));
        lore.add(Component.literal("§7§m------------------------"));
        paper.set(DataComponents.LORE, new ItemLore(lore));

        // Return placed sample items
        ItemStack refundPrice = priceStack.copy();
        ItemStack refundSale = saleStack.copy();
        getContainer().setItem(PRICE_SLOT, ItemStack.EMPTY);
        getContainer().setItem(SALE_SLOT, ItemStack.EMPTY);

        if (!refundPrice.isEmpty()) {
            player.getInventory().placeItemBackInInventory(refundPrice, Prediction.SERVER_ONLY);
        }
        if (!refundSale.isEmpty()) {
            player.getInventory().placeItemBackInInventory(refundSale, Prediction.SERVER_ONLY);
        }

        // Give paper
        if (!player.getInventory().add(paper)) {
            player.drop(paper, false, Prediction.SERVER_ONLY);
        }

        if (player.level() != null) {
            player.level().playSound(null, player.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 1.0f, 1.2f);
        }
        player.sendSystemMessage(Component.literal("§a✔ Created Shop Paper: §e\"" + generatedTradeText + "\" §a(placed in inventory)!"));

        // Close creator menu
        if (player instanceof ServerPlayer sp) {
            sp.closeContainer();
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slotIndex) {
        return ItemStack.EMPTY;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        // Safely refund any items in the price or sale slot when closed
        ItemStack price = getContainer().getItem(PRICE_SLOT);
        if (!price.isEmpty()) {
            player.getInventory().placeItemBackInInventory(price, Prediction.SERVER_ONLY);
            getContainer().setItem(PRICE_SLOT, ItemStack.EMPTY);
        }
        ItemStack sale = getContainer().getItem(SALE_SLOT);
        if (!sale.isEmpty()) {
            player.getInventory().placeItemBackInInventory(sale, Prediction.SERVER_ONLY);
            getContainer().setItem(SALE_SLOT, ItemStack.EMPTY);
        }
    }

    @Override
    public boolean stillValid(Player player) {
        return player.isAlive();
    }

    public static ItemStack createGuiItem(Item item, Component name, List<Component> lore) {
        ItemStack stack = new ItemStack(item);
        stack.set(DataComponents.CUSTOM_NAME, name);
        if (lore != null && !lore.isEmpty()) {
            stack.set(DataComponents.LORE, new ItemLore(lore));
        }
        return stack;
    }
}
