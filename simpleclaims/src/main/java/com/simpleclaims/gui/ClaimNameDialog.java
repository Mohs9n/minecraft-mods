package com.simpleclaims.gui;

import com.mojang.serialization.JavaOps;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.server.dialog.ActionButton;
import net.minecraft.server.dialog.CommonButtonData;
import net.minecraft.server.dialog.CommonDialogData;
import net.minecraft.server.dialog.DialogAction;
import net.minecraft.server.dialog.Input;
import net.minecraft.server.dialog.MultiActionDialog;
import net.minecraft.server.dialog.action.Action;
import net.minecraft.server.dialog.action.CommandTemplate;
import net.minecraft.server.dialog.action.ParsedTemplate;
import net.minecraft.server.dialog.input.TextInput;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;
import java.util.Optional;

/**
 * A native vanilla "Dialog" popup (Minecraft's own server-driven settings/confirmation UI)
 * used to name a claim - a clean, professional-looking screen with a real text field and
 * buttons, no anvil re-purposing, no leftover item in the inventory, and no custom
 * client-side rendering code at all (every vanilla client already knows how to draw this).
 *
 * Confirming runs the existing "/claim create &lt;name&gt;" command with the typed text
 * substituted in via $(name), so it goes through the exact same validation (selection
 * check, overlap check, claim limits) as typing the command would. Because the command
 * parses the name as a single Brigadier "word" argument, spaces aren't supported here
 * either - same restriction the text command already had.
 */
public final class ClaimNameDialog {

    private ClaimNameDialog() {
    }

    public static void open(ServerPlayer player) {
        TextInput nameInput = new TextInput(200, Component.literal("Claim Name (single word)"), true, "MyClaim", 32, Optional.empty());

        CommonDialogData common = new CommonDialogData(
                Component.literal("§6§lName Your Claim"),
                Optional.empty(),
                true,
                false,
                DialogAction.CLOSE,
                List.of(),
                List.of(new Input("name", nameInput))
        );

        Action createAction = new CommandTemplate(parseTemplate("claim create $(name)"));
        ActionButton createButton = new ActionButton(
                new CommonButtonData(Component.literal("§a§lCreate Claim"), 150),
                Optional.of(createAction)
        );
        ActionButton cancelButton = new ActionButton(
                new CommonButtonData(Component.literal("§cCancel"), 100),
                Optional.empty()
        );

        MultiActionDialog dialog = new MultiActionDialog(common, List.of(createButton, cancelButton), Optional.empty(), 2);
        player.openDialog(Holder.direct(dialog));
    }

    private static ParsedTemplate parseTemplate(String raw) {
        return ParsedTemplate.CODEC.parse(JavaOps.INSTANCE, raw).getOrThrow();
    }
}
