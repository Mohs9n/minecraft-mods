package com.simpleclaims.gui;

import com.simpleclaims.Claim;
import com.simpleclaims.ClaimManager;
import com.simpleclaims.command.ClaimCommands;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
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

import java.util.ArrayList;
import java.util.List;

/**
 * Graphical entry point for /claim gui: lists every claim the player owns.
 * Clicking a claim opens {@link ClaimDetailMenu} to manage it without typing commands.
 */
public class ClaimListMenu extends ChestMenu {

    private static final int SIZE = 54;
    private static final int MAX_SHOWN = 45; // rows 0-4; row 5 is reserved for quick actions
    private static final int WAND_SLOT = 45;
    private static final int CREATE_SLOT = 47;
    private static final int FOOTER_SLOT = 49;
    private static final int HELP_SLOT = 53;

    private final List<Claim> claims;

    public ClaimListMenu(int containerId, Inventory playerInventory) {
        super(MenuType.GENERIC_9x6, containerId, playerInventory, new SimpleContainer(SIZE), 6);
        this.claims = ClaimManager.getClaimsByOwner(playerInventory.player.getUUID());
        render(playerInventory.player);
    }

    public static void open(ServerPlayer player) {
        player.openMenu(new SimpleMenuProvider(
                (containerId, playerInv, p) -> new ClaimListMenu(containerId, playerInv),
                Component.literal("§6Your Claims")
        ));
    }

    private void render(Player player) {
        ItemStack border = createGuiItem(Items.STAINED_GLASS_PANE.gray(), Component.literal("§r"), null);
        for (int i = 0; i < SIZE; i++) {
            getContainer().setItem(i, border.copy());
        }

        if (claims.isEmpty()) {
            getContainer().setItem(22, createGuiItem(
                    Items.BARRIER,
                    Component.literal("§cNo Claims Yet"),
                    List.of(Component.literal("§7Get the wand below and claim some land!"))
            ));
        } else {
            int shown = Math.min(claims.size(), MAX_SHOWN);
            for (int i = 0; i < shown; i++) {
                Claim c = claims.get(i);
                List<Component> lore = new ArrayList<>();
                lore.add(Component.literal("§7Dimension: §f" + c.getDimension()));
                lore.add(Component.literal("§7Bounds: §f(" + c.getMinX() + ", " + c.getMinZ() + ") §7to §f(" + c.getMaxX() + ", " + c.getMaxZ() + ")"));
                lore.add(Component.literal("§7Size: §b" + c.getWidthX() + "x" + c.getWidthZ() + " §7(" + c.getArea() + " blocks)"));
                lore.add(Component.literal("§7Trusted: §a" + c.getMembers().size() + " player(s)"));
                lore.add(Component.literal("§eClick to manage"));
                getContainer().setItem(i, createGuiItem(Items.PAPER, Component.literal("§6§l" + c.getName()), lore));
            }
        }

        // --- Quick action row (row 5) ---
        getContainer().setItem(WAND_SLOT, createGuiItem(
                Items.GOLDEN_HOE,
                Component.literal("§6§lGet Claim Wand"),
                List.of(
                        Component.literal("§7Left-click a block: §eSet Corner 1"),
                        Component.literal("§7Right-click a block: §eSet Corner 2"),
                        Component.literal("§7Sneak + right-click: §eOpen this menu"),
                        Component.literal("§eClick to receive the wand (closes this menu)")
                )
        ));

        String dim = ClaimManager.getDimensionId(player.level());
        ClaimManager.PlayerSelection sel = ClaimManager.getSelection(player.getUUID(), dim);
        if (sel.isComplete()) {
            getContainer().setItem(CREATE_SLOT, createGuiItem(
                    Items.EMERALD,
                    Component.literal("§a§lCreate Claim From Selection"),
                    List.of(
                            Component.literal("§7Selection size: §b" + sel.getWidthX() + "x" + sel.getWidthZ() + " §7(" + sel.getArea() + " blocks)"),
                            Component.literal("§eClick to name and create it!")
                    )
            ));
        } else {
            getContainer().setItem(CREATE_SLOT, createGuiItem(
                    Items.MAP,
                    Component.literal("§7Create Claim From Selection"),
                    List.of(
                            Component.literal("§7Get the wand and set both corners first."),
                            Component.literal("§8(Left-click = Corner 1, Right-click = Corner 2)")
                    )
            ));
        }

        long ownedArea = 0L;
        for (Claim c : claims) {
            ownedArea += c.getArea();
        }
        List<Component> footerLore = new ArrayList<>();
        footerLore.add(Component.literal("§7Claims: §f" + claims.size() + " §7/ §f" + ClaimManager.MAX_CLAIMS_PER_PLAYER));
        footerLore.add(Component.literal("§7Area: §f" + ownedArea + " §7/ §f" + ClaimManager.MAX_TOTAL_CLAIM_AREA_PER_PLAYER + " blocks"));
        if (claims.size() > MAX_SHOWN) {
            footerLore.add(Component.literal("§8Only the first " + MAX_SHOWN + " are shown here."));
        }
        getContainer().setItem(FOOTER_SLOT, createGuiItem(
                Items.WRITABLE_BOOK,
                Component.literal("§7You own §e" + claims.size() + " §7claim(s)"),
                footerLore
        ));

        getContainer().setItem(HELP_SLOT, createGuiItem(
                Items.BOOK,
                Component.literal("§b§lHelp"),
                List.of(
                        Component.literal("§7/claim trust <player> §8- also in each claim's menu"),
                        Component.literal("§7/claim untrust <player> §8- also in each claim's menu"),
                        Component.literal("§7/claim delete §8- also in each claim's menu"),
                        Component.literal("§7/claim list, /claim info §8- this menu replaces them")
                )
        ));
    }

    @Override
    public void clicked(int slotIndex, int button, ContainerInput input, Player player) {
        if (slotIndex >= 0 && slotIndex < SIZE) {
            if (slotIndex < claims.size() && slotIndex < MAX_SHOWN && player instanceof ServerPlayer sp) {
                ClaimDetailMenu.open(sp, claims.get(slotIndex).getId());
                return;
            }

            if (slotIndex == WAND_SLOT && player instanceof ServerPlayer sp) {
                ClaimCommands.giveWand(sp);
                sp.closeContainer();
                return;
            }

            if (slotIndex == CREATE_SLOT && player instanceof ServerPlayer sp) {
                String dim = ClaimManager.getDimensionId(player.level());
                if (ClaimManager.getSelection(player.getUUID(), dim).isComplete()) {
                    ClaimNameDialog.open(sp);
                }
                return;
            }

            sendAllDataToRemote();
            return;
        }

        if (slotIndex >= SIZE) {
            if (input == ContainerInput.QUICK_MOVE) {
                sendAllDataToRemote();
                return;
            }
            super.clicked(slotIndex, button, input, player);
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slotIndex) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return player.isAlive();
    }

    static ItemStack createGuiItem(Item item, Component name, List<Component> lore) {
        ItemStack stack = new ItemStack(item);
        stack.set(DataComponents.CUSTOM_NAME, name);
        if (lore != null && !lore.isEmpty()) {
            stack.set(DataComponents.LORE, new ItemLore(lore));
        }
        return stack;
    }
}
