package com.simpleclaims;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.numbers.BlankFormat;
import net.minecraft.network.protocol.game.ClientboundSetDisplayObjectivePacket;
import net.minecraft.network.protocol.game.ClientboundSetObjectivePacket;
import net.minecraft.network.protocol.game.ClientboundSetScorePacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;

import java.util.Optional;

/**
 * A per-player sidebar built from raw scoreboard packets sent only to that one player's
 * own connection. It never touches the server's shared Scoreboard/Objective registry, so
 * there's no cross-player name collision to manage and nothing to clean up server-side -
 * the client just discards it on disconnect.
 *
 * This replaces the old chat-based "Entering/Leaving claim" spam: the claim line updates
 * in place instead of printing a new line to chat every time a player crosses a border.
 * The money line is a placeholder for a future economy - it isn't wired to anything yet.
 */
public final class ClaimSidebar {

    private static final String OBJECTIVE_NAME = "simpleclaims_hud";
    private static final String LINE_USER = "user";
    private static final String LINE_MONEY = "money";
    private static final String LINE_CLAIM = "claim";

    private ClaimSidebar() {
    }

    public static void show(ServerPlayer player) {
        Objective objective = new Objective(
                player.level().getScoreboard(),
                OBJECTIVE_NAME,
                ObjectiveCriteria.DUMMY,
                Component.literal("§6§lSimpleClaims"),
                ObjectiveCriteria.RenderType.INTEGER,
                false,
                BlankFormat.INSTANCE
        );
        player.connection.send(new ClientboundSetObjectivePacket(objective, ClientboundSetObjectivePacket.METHOD_ADD));
        player.connection.send(new ClientboundSetDisplayObjectivePacket(DisplaySlot.SIDEBAR, objective));

        setLine(player, LINE_USER, 3, Component.literal("§7User: §f" + player.getName().getString()));
        setLine(player, LINE_MONEY, 2, Component.literal("§7Money: §a$0"));
        updateClaimLine(player);
    }

    /**
     * Refreshes just the claim-status line. Safe to call every tick a transition is
     * confirmed - it's a single small packet, not a full sidebar rebuild.
     */
    public static void updateClaimLine(ServerPlayer player) {
        Claim claim = ClaimManager.getClaimAt(player.level(), player.blockPosition());
        Component text = claim != null
                ? Component.literal("§7Claim: §e" + claim.getName())
                : Component.literal("§7Claim: §8Wilderness");
        setLine(player, LINE_CLAIM, 1, text);
    }

    private static void setLine(ServerPlayer player, String owner, int score, Component display) {
        player.connection.send(new ClientboundSetScorePacket(
                owner, OBJECTIVE_NAME, score, Optional.of(display), Optional.of(BlankFormat.INSTANCE)
        ));
    }
}
