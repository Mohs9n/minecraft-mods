package com.simplechestshop.gui;

import com.simplechestshop.ChestShopManager;
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
    public static final int PRICE_1_SLOT = 10;
    public static final int PRICE_2_SLOT = 11;
    public static final int PRICE_3_SLOT = 12;
    public static final int ARROW_1_SLOT = 13;
    public static final int SALE_SLOT = 14;
    public static final int ARROW_2_SLOT = 15;
    public static final int CREATE_BUTTON_SLOT = 16;

    public static final int PRICE_1_INFO_SLOT = 1;
    public static final int PRICE_2_INFO_SLOT = 2;
    public static final int PRICE_3_INFO_SLOT = 3;
    public static final int SALE_INFO_SLOT = 5;
    public static final int CREATE_INFO_SLOT = 7;

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
                Component.literal("§6Create Shop Paper §8(Multi-Price)")
        ));
    }

    private void setupGui() {
        ItemStack border = createGuiItem(Items.STAINED_GLASS_PANE.gray(), Component.literal("§r"), null);
        for (int i = 0; i < 27; i++) {
            if (isEditableSlot(i)) {
                continue;
            }
            getContainer().setItem(i, border.copy());
        }

        // Row 0 Info icons
        ItemStack price1Info = createGuiItem(
                Items.GOLD_INGOT,
                Component.literal("§6§lPrice 1 §7(Primary)"),
                List.of(
                        Component.literal("§7Place your main price/currency in the slot below."),
                        Component.literal("§eExample: 1x Diamond")
                )
        );
        getContainer().setItem(PRICE_1_INFO_SLOT, price1Info);

        ItemStack price2Info = createGuiItem(
                Items.IRON_INGOT,
                Component.literal("§f§lPrice 2 §7(Alternative - Optional)"),
                List.of(
                        Component.literal("§7Place an optional 2nd price in the slot below."),
                        Component.literal("§eExample: 10x Iron Ingot"),
                        Component.literal("§8Buyers can choose either price!")
                )
        );
        getContainer().setItem(PRICE_2_INFO_SLOT, price2Info);

        ItemStack price3Info = createGuiItem(
                Items.COPPER_INGOT,
                Component.literal("§c§lPrice 3 §7(Alternative - Optional)"),
                List.of(
                        Component.literal("§7Place an optional 3rd price in the slot below."),
                        Component.literal("§eExample: 32x Coal"),
                        Component.literal("§8Buyers can choose any configured price!")
                )
        );
        getContainer().setItem(PRICE_3_INFO_SLOT, price3Info);

        ItemStack saleInfo = createGuiItem(
                Items.CHEST,
                Component.literal("§b§lSale Slot §7(Item to Sell)"),
                List.of(
                        Component.literal("§7Place the item you want to sell below."),
                        Component.literal("§8Optional: if empty, sells any items in chest.")
                )
        );
        getContainer().setItem(SALE_INFO_SLOT, saleInfo);

        ItemStack createInfo = createGuiItem(
                Items.WRITABLE_BOOK,
                Component.literal("§a§lCreate Paper"),
                List.of(
                        Component.literal("§7Click the button below when ready to generate!"),
                        Component.literal("§7Sample items will be safely returned.")
                )
        );
        getContainer().setItem(CREATE_INFO_SLOT, createInfo);

        // Separator arrows in row 1
        ItemStack arrow1 = createGuiItem(Items.STAINED_GLASS_PANE.lightBlue(), Component.literal("§b➡ FOR ➡"), null);
        getContainer().setItem(ARROW_1_SLOT, arrow1);

        ItemStack arrow2 = createGuiItem(Items.STAINED_GLASS_PANE.lime(), Component.literal("§a➡"), null);
        getContainer().setItem(ARROW_2_SLOT, arrow2);

        // Create Button
        ItemStack createBtn = createGuiItem(
                Items.EMERALD,
                Component.literal("§a§l[ Create Shop Paper ]"),
                List.of(
                        Component.literal("§7Click here to generate your shop paper!"),
                        Component.literal("§7Your sample items will be safely returned."),
                        Component.literal("§eRequires at least Price 1.")
                )
        );
        getContainer().setItem(CREATE_BUTTON_SLOT, createBtn);
    }

    private boolean isEditableSlot(int slot) {
        return slot == PRICE_1_SLOT || slot == PRICE_2_SLOT || slot == PRICE_3_SLOT || slot == SALE_SLOT;
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

            // Only designated slots are editable by player
            if (isEditableSlot(slotIndex)) {
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
                    if (getContainer().getItem(PRICE_1_SLOT).isEmpty()) {
                        getContainer().setItem(PRICE_1_SLOT, moving.copy());
                        slot.set(ItemStack.EMPTY);
                    } else if (getContainer().getItem(SALE_SLOT).isEmpty()) {
                        getContainer().setItem(SALE_SLOT, moving.copy());
                        slot.set(ItemStack.EMPTY);
                    } else if (getContainer().getItem(PRICE_2_SLOT).isEmpty()) {
                        getContainer().setItem(PRICE_2_SLOT, moving.copy());
                        slot.set(ItemStack.EMPTY);
                    } else if (getContainer().getItem(PRICE_3_SLOT).isEmpty()) {
                        getContainer().setItem(PRICE_3_SLOT, moving.copy());
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
        ItemStack p1 = getContainer().getItem(PRICE_1_SLOT);
        ItemStack p2 = getContainer().getItem(PRICE_2_SLOT);
        ItemStack p3 = getContainer().getItem(PRICE_3_SLOT);
        ItemStack saleStack = getContainer().getItem(SALE_SLOT);

        if (p1.isEmpty()) {
            player.sendSystemMessage(Component.literal("§c[Shop Creator] Please place at least one price item in Price Slot 1 (Primary)!"));
            if (player.level() != null) {
                player.level().playSound(null, player.blockPosition(), SoundEvents.VILLAGER_NO, SoundSource.PLAYERS, 1.0f, 1.0f);
            }
            return;
        }

        List<ItemStack> priceStacks = new ArrayList<>();
        priceStacks.add(p1);
        if (!p2.isEmpty()) priceStacks.add(p2);
        if (!p3.isEmpty()) priceStacks.add(p3);

        List<String> priceParts = new ArrayList<>();
        for (ItemStack ps : priceStacks) {
            Identifier id = BuiltInRegistries.ITEM.getKey(ps.getItem());
            priceParts.add(ps.getCount() + " " + id.getPath());
        }

        String pricesText = String.join(" or ", priceParts);
        String generatedTradeText;

        if (!saleStack.isEmpty()) {
            Identifier saleId = BuiltInRegistries.ITEM.getKey(saleStack.getItem());
            int saleCount = saleStack.getCount();
            generatedTradeText = pricesText + " -> " + saleCount + " " + saleId.getPath();
        } else {
            generatedTradeText = pricesText;
        }

        // Create the paper
        ItemStack paper = new ItemStack(Items.PAPER);
        paper.set(DataComponents.CUSTOM_NAME, Component.literal(generatedTradeText));

        List<Component> lore = new ArrayList<>();
        lore.add(Component.literal("§7§m------------------------"));
        lore.add(Component.literal("§6Chest Shop Config Paper"));
        for (int i = 0; i < priceStacks.size(); i++) {
            ItemStack ps = priceStacks.get(i);
            lore.add(Component.literal("§7Price " + (i + 1) + ": §b" + ps.getCount() + "x ")
                    .append(ChestShopManager.getItemComponent(ps.getItem()).copy().withStyle(net.minecraft.ChatFormatting.AQUA)));
        }
        if (!saleStack.isEmpty()) {
            lore.add(Component.literal("§7Selling: §f" + saleStack.getCount() + "x ")
                    .append(ChestShopManager.getItemComponent(saleStack.getItem()).copy().withStyle(net.minecraft.ChatFormatting.WHITE)));
        } else {
            lore.add(Component.literal("§7Selling: §f[Any Chest Items]"));
        }
        lore.add(Component.literal("§ePlace this in a chest to activate!"));
        lore.add(Component.literal("§7§m------------------------"));
        paper.set(DataComponents.LORE, new ItemLore(lore));

        // Return placed sample items
        ItemStack refundP1 = p1.copy();
        ItemStack refundP2 = p2.copy();
        ItemStack refundP3 = p3.copy();
        ItemStack refundSale = saleStack.copy();

        getContainer().setItem(PRICE_1_SLOT, ItemStack.EMPTY);
        getContainer().setItem(PRICE_2_SLOT, ItemStack.EMPTY);
        getContainer().setItem(PRICE_3_SLOT, ItemStack.EMPTY);
        getContainer().setItem(SALE_SLOT, ItemStack.EMPTY);

        if (!refundP1.isEmpty()) player.getInventory().placeItemBackInInventory(refundP1, Prediction.SERVER_ONLY);
        if (!refundP2.isEmpty()) player.getInventory().placeItemBackInInventory(refundP2, Prediction.SERVER_ONLY);
        if (!refundP3.isEmpty()) player.getInventory().placeItemBackInInventory(refundP3, Prediction.SERVER_ONLY);
        if (!refundSale.isEmpty()) player.getInventory().placeItemBackInInventory(refundSale, Prediction.SERVER_ONLY);

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
        // Safely refund any items in editable slots when closed
        int[] editable = {PRICE_1_SLOT, PRICE_2_SLOT, PRICE_3_SLOT, SALE_SLOT};
        for (int s : editable) {
            ItemStack st = getContainer().getItem(s);
            if (!st.isEmpty()) {
                player.getInventory().placeItemBackInInventory(st, Prediction.SERVER_ONLY);
                getContainer().setItem(s, ItemStack.EMPTY);
            }
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
