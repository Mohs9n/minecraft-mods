package com.simpleclaims.gui;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AnvilMenu;
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
        seed.set(DataComponents.LORE, new ItemLore(List.of(Component.literal("§7Type a claim name above, then take this paper to confirm."))));
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
        if (completed) return;
        completed = true;

        Component nameComponent = stack.get(DataComponents.CUSTOM_NAME);
        String finalName = (nameComponent != null && !nameComponent.getString().isBlank())
                ? nameComponent.getString() : "My Claim";

        if (player instanceof ServerPlayer sp) {
            callback.onNamed(sp, finalName);
        }
    }

    @Override
    public boolean stillValid(Player player) {
        return player == owner && player.isAlive();
    }
}
