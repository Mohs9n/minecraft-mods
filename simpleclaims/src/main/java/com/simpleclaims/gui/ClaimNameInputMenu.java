package com.simpleclaims.gui;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemLore;

import java.util.List;

/**
 * Repurposes the vanilla anvil screen as a free text-input box for naming a claim from
 * the GUI, instead of requiring the player to type "/claim create &lt;name&gt;" in chat.
 * No XP/material cost is ever charged - createResult()/mayPickup() bypass the normal
 * repair-cost logic entirely.
 */
public class ClaimNameInputMenu extends AnvilMenu {

    public interface NameCallback {
        void onNamed(ServerPlayer player, String name);
    }

    private final NameCallback callback;
    private final Player owner;
    private boolean completed = false;
    private String typedName = "";

    public ClaimNameInputMenu(int containerId, Inventory playerInventory, NameCallback callback) {
        super(containerId, playerInventory, ContainerLevelAccess.NULL);
        this.callback = callback;
        this.owner = playerInventory.player;

        ItemStack seed = new ItemStack(Items.PAPER);
        seed.set(DataComponents.CUSTOM_NAME, Component.literal("My Claim"));
        seed.set(DataComponents.LORE, new ItemLore(List.of(Component.literal("§7Type a claim name above, then click the arrow slot to confirm."))));
        getSlot(AnvilMenu.INPUT_SLOT).set(seed);
        this.typedName = "My Claim";
    }

    public static void open(ServerPlayer player, NameCallback callback) {
        player.openMenu(new SimpleMenuProvider(
                (containerId, playerInv, p) -> new ClaimNameInputMenu(containerId, playerInv, callback),
                Component.literal("Name Your Claim")
        ));
    }

    @Override
    public boolean setItemName(String name) {
        this.typedName = (name == null) ? "" : name;
        boolean changed = super.setItemName(name);
        createResult();
        return changed;
    }

    @Override
    public void createResult() {
        ItemStack result = new ItemStack(Items.PAPER);
        String display = (typedName == null || typedName.isBlank()) ? "My Claim" : typedName;
        result.set(DataComponents.CUSTOM_NAME, Component.literal(display));
        resultSlots.setItem(0, result);
    }

    @Override
    protected boolean mayPickup(Player player, boolean hasSecondItem) {
        return true;
    }

    @Override
    protected void onTake(Player player, ItemStack stack) {
        // Intentionally empty: clicked() below handles confirmation and never lets the
        // vanilla "give the result item to the player" transfer happen in the first place,
        // so there is nothing left here to do.
    }

    /**
     * Clicking the result slot confirms the name WITHOUT ever letting the paper reach the
     * player's cursor/inventory - we never call super.clicked() for that slot, so vanilla's
     * item-transfer logic for an anvil's result never runs. The input/additional slots are
     * frozen (nothing to rearrange); clicks there and in the player inventory are otherwise
     * harmless no-ops except shift-click, which is blocked outright.
     */
    @Override
    public void clicked(int slotIndex, int button, ContainerInput input, Player player) {
        if (slotIndex == AnvilMenu.RESULT_SLOT) {
            if (!completed) {
                completed = true;
                String finalName = (typedName == null || typedName.isBlank()) ? "My Claim" : typedName;
                if (player instanceof ServerPlayer sp) {
                    sp.closeContainer();
                    callback.onNamed(sp, finalName);
                }
            }
            return;
        }

        if (slotIndex == AnvilMenu.INPUT_SLOT || slotIndex == AnvilMenu.ADDITIONAL_SLOT) {
            sendAllDataToRemote();
            return;
        }

        if (input == ContainerInput.QUICK_MOVE) {
            sendAllDataToRemote();
            return;
        }
        super.clicked(slotIndex, button, input, player);
    }

    @Override
    public boolean stillValid(Player player) {
        return player == owner && player.isAlive();
    }

    @Override
    public void removed(Player player) {
        // ItemCombinerMenu.removed() normally returns whatever's in the input slots to the
        // player when the menu closes (so you get your materials back if you abandon an
        // anvil repair). Our "input" is just a placeholder paper for the rename UI, not a
        // real item - clear it first so it never gets handed back.
        inputSlots.setItem(AnvilMenu.INPUT_SLOT, ItemStack.EMPTY);
        inputSlots.setItem(AnvilMenu.ADDITIONAL_SLOT, ItemStack.EMPTY);
        super.removed(player);
    }
}
