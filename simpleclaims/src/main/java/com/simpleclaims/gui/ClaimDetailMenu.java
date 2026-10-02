package com.simpleclaims.gui;

import com.simpleclaims.Claim;
import com.simpleclaims.ClaimManager;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ResolvableProfile;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Manage a single claim: view info, add/remove trusted members from a point-and-click
 * list of online players, and delete the claim (with a one-click "arm" confirmation).
 */
public class ClaimDetailMenu extends ChestMenu {

    private static final int SIZE = 54;
    private static final int INFO_SLOT = 4;
    private static final int ADD_TRUSTED_SLOT = 38;
    private static final int DELETE_SLOT = 40;
    private static final int BACK_SLOT = 45;
    private static final int MEMBER_START = 9;
    private static final int MEMBER_END = 35; // inclusive: 27 slots for members/candidates

    private final String claimId;
    private boolean pickMode = false;
    private boolean deleteArmed = false;
    private final List<UUID> pickCandidates = new ArrayList<>();

    public ClaimDetailMenu(int containerId, Inventory playerInventory, String claimId) {
        super(MenuType.GENERIC_9x6, containerId, playerInventory, new SimpleContainer(SIZE), 6);
        this.claimId = claimId;
        render(playerInventory.player);
    }

    public static void open(ServerPlayer player, String claimId) {
        player.openMenu(new SimpleMenuProvider(
                (containerId, playerInv, p) -> new ClaimDetailMenu(containerId, playerInv, claimId),
                Component.literal("§6Manage Claim")
        ));
    }

    private Claim claim() {
        return ClaimManager.getClaimById(claimId);
    }

    private void render(Player player) {
        ItemStack border = ClaimListMenu.createGuiItem(Items.STAINED_GLASS_PANE.gray(), Component.literal("§r"), null);
        for (int i = 0; i < SIZE; i++) {
            getContainer().setItem(i, border.copy());
        }

        Claim c = claim();
        if (c == null) {
            getContainer().setItem(22, ClaimListMenu.createGuiItem(Items.BARRIER, Component.literal("§cThis claim no longer exists."), null));
            return;
        }

        boolean isOwner = c.isOwner(player.getUUID()) || ClaimManager.isOpOrAdmin(player);

        List<Component> infoLore = new ArrayList<>();
        infoLore.add(Component.literal("§7Owner: §e" + c.getOwnerName()));
        infoLore.add(Component.literal("§7Dimension: §f" + c.getDimension()));
        infoLore.add(Component.literal("§7Bounds: §f(" + c.getMinX() + ", " + c.getMinZ() + ") §7to §f(" + c.getMaxX() + ", " + c.getMaxZ() + ")"));
        infoLore.add(Component.literal("§7Size: §b" + c.getWidthX() + "x" + c.getWidthZ() + " §7(" + c.getArea() + " blocks)"));
        infoLore.add(Component.literal("§7Height: §aAll Y levels (Bedrock to Sky)"));
        getContainer().setItem(INFO_SLOT, ClaimListMenu.createGuiItem(Items.PAPER, Component.literal("§6§l" + c.getName()), infoLore));

        if (pickMode) {
            renderPickMode(player, c);
        } else {
            renderMemberList(c);
        }

        if (isOwner) {
            getContainer().setItem(ADD_TRUSTED_SLOT, ClaimListMenu.createGuiItem(
                    pickMode ? Items.REDSTONE : Items.EMERALD,
                    Component.literal(pickMode ? "§c« Cancel" : "§a+ Add Trusted Player"),
                    List.of(Component.literal(pickMode ? "§7Return to the member list." : "§7Pick an online player to trust."))
            ));

            getContainer().setItem(DELETE_SLOT, ClaimListMenu.createGuiItem(
                    deleteArmed ? Items.TNT : Items.BARRIER,
                    Component.literal(deleteArmed ? "§c§lCLICK AGAIN TO CONFIRM DELETE" : "§c§lDelete Claim"),
                    List.of(Component.literal("§7This cannot be undone!"))
            ));
        }

        getContainer().setItem(BACK_SLOT, ClaimListMenu.createGuiItem(Items.ARROW, Component.literal("§e« Back to Claim List"), null));
    }

    private void renderMemberList(Claim c) {
        List<Map.Entry<String, String>> members = new ArrayList<>(c.getMembers().entrySet());
        int slot = MEMBER_START;
        for (Map.Entry<String, String> entry : members) {
            if (slot > MEMBER_END) break;
            ItemStack head = ClaimListMenu.createGuiItem(
                    Items.PLAYER_HEAD,
                    Component.literal("§e" + entry.getValue()),
                    List.of(Component.literal("§7Trusted member"), Component.literal("§cClick to remove"))
            );
            try {
                head.set(DataComponents.PROFILE, ResolvableProfile.createUnresolved(UUID.fromString(entry.getKey())));
            } catch (IllegalArgumentException ignored) {
            }
            getContainer().setItem(slot, head);
            slot++;
        }
        if (members.isEmpty()) {
            getContainer().setItem(MEMBER_START + 4, ClaimListMenu.createGuiItem(Items.BOOK, Component.literal("§7No trusted members yet"), null));
        }
    }

    private void renderPickMode(Player player, Claim c) {
        pickCandidates.clear();
        if (player.level().getServer() == null) return;
        int slot = MEMBER_START;
        for (ServerPlayer online : player.level().getServer().getPlayerList().getPlayers()) {
            if (slot > MEMBER_END) break;
            UUID uuid = online.getUUID();
            if (c.isOwner(uuid) || c.isMember(uuid)) continue;
            pickCandidates.add(uuid);
            ItemStack head = ClaimListMenu.createGuiItem(
                    Items.PLAYER_HEAD,
                    Component.literal("§a" + online.getName().getString()),
                    List.of(Component.literal("§7Click to trust this player"))
            );
            head.set(DataComponents.PROFILE, ResolvableProfile.createResolved(online.getGameProfile()));
            getContainer().setItem(slot, head);
            slot++;
        }
        if (pickCandidates.isEmpty()) {
            getContainer().setItem(MEMBER_START + 4, ClaimListMenu.createGuiItem(Items.BOOK, Component.literal("§7No eligible online players"), null));
        }
    }

    @Override
    public void clicked(int slotIndex, int button, ContainerInput input, Player player) {
        if (slotIndex < 0 || slotIndex >= SIZE) {
            if (slotIndex >= SIZE) {
                if (input == ContainerInput.QUICK_MOVE) {
                    sendAllDataToRemote();
                    return;
                }
                super.clicked(slotIndex, button, input, player);
            }
            return;
        }

        Claim c = claim();
        if (c == null) {
            sendAllDataToRemote();
            return;
        }
        boolean isOwner = c.isOwner(player.getUUID()) || ClaimManager.isOpOrAdmin(player);

        if (slotIndex == BACK_SLOT) {
            if (player instanceof ServerPlayer sp) {
                ClaimListMenu.open(sp);
            }
            return;
        }

        if (!isOwner) {
            sendAllDataToRemote();
            return;
        }

        if (slotIndex == ADD_TRUSTED_SLOT) {
            pickMode = !pickMode;
            deleteArmed = false;
            render(player);
            sendAllDataToRemote();
            return;
        }

        if (slotIndex == DELETE_SLOT) {
            if (!deleteArmed) {
                deleteArmed = true;
                render(player);
                sendAllDataToRemote();
                return;
            }
            String deletedName = c.getName();
            if (player.level() instanceof ServerLevel sl) {
                ClaimManager.removeClaim(sl, c);
            }
            player.sendSystemMessage(Component.literal("§a✔ Deleted claim §e\"" + deletedName + "\"§a."));
            if (player instanceof ServerPlayer sp) {
                ClaimListMenu.open(sp);
            }
            return;
        }

        if (slotIndex >= MEMBER_START && slotIndex <= MEMBER_END) {
            int idx = slotIndex - MEMBER_START;
            if (pickMode) {
                if (idx < pickCandidates.size() && player.level().getServer() != null) {
                    UUID target = pickCandidates.get(idx);
                    ServerPlayer targetPlayer = player.level().getServer().getPlayerList().getPlayer(target);
                    String name = targetPlayer != null ? targetPlayer.getName().getString() : "Unknown";
                    c.addMember(target, name);
                    if (player.level() instanceof ServerLevel sl) {
                        ClaimManager.save(sl);
                    }
                    player.sendSystemMessage(Component.literal("§a✔ Trusted §e" + name + " §ain this claim!"));
                    if (targetPlayer != null) {
                        targetPlayer.sendSystemMessage(Component.literal("§a✔ You were trusted in §e" + player.getName().getString() + "§a's claim (§6" + c.getName() + "§a)!"));
                    }
                }
                pickMode = false;
            } else {
                List<Map.Entry<String, String>> members = new ArrayList<>(c.getMembers().entrySet());
                if (idx < members.size()) {
                    try {
                        UUID target = UUID.fromString(members.get(idx).getKey());
                        c.removeMember(target);
                        if (player.level() instanceof ServerLevel sl) {
                            ClaimManager.save(sl);
                        }
                        player.sendSystemMessage(Component.literal("§e✔ Removed §f" + members.get(idx).getValue() + " §efrom this claim."));
                    } catch (IllegalArgumentException ignored) {
                    }
                }
            }
            deleteArmed = false;
            render(player);
            sendAllDataToRemote();
            return;
        }

        deleteArmed = false;
        sendAllDataToRemote();
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slotIndex) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return player.isAlive();
    }
}
