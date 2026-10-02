package com.simplechestshop.command;

import com.mojang.brigadier.CommandDispatcher;
import com.simplechestshop.gui.ShopCreationMenu;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public class ShopCommands {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        // /shopcreate
        dispatcher.register(Commands.literal("shopcreate")
                .executes(context -> {
                    ServerPlayer player = context.getSource().getPlayerOrException();
                    ShopCreationMenu.open(player);
                    return 1;
                })
        );

        // /shop and subcommands
        dispatcher.register(Commands.literal("shop")
                .then(Commands.literal("create")
                        .executes(context -> {
                            ServerPlayer player = context.getSource().getPlayerOrException();
                            ShopCreationMenu.open(player);
                            return 1;
                        })
                )
                .then(Commands.literal("help")
                        .executes(context -> {
                            ServerPlayer player = context.getSource().getPlayerOrException();
                            sendHelp(player);
                            return 1;
                        })
                )
                .executes(context -> {
                    ServerPlayer player = context.getSource().getPlayerOrException();
                    ShopCreationMenu.open(player);
                    return 1;
                })
        );
    }

    private static void sendHelp(ServerPlayer player) {
        player.sendSystemMessage(Component.literal("§6============= §e[Simple Chest Shop] §6============="));
        player.sendSystemMessage(Component.literal("§e/shopcreate §7or §e/shop create§f: Open graphical UI to create shop paper."));
        player.sendSystemMessage(Component.literal("§eRight-Click Shop Chest§f: Open graphical Buy Menu to purchase goods."));
        player.sendSystemMessage(Component.literal("§eLeft-Click (Punch) Chest§f: View quick price and stock summary in chat."));
        player.sendSystemMessage(Component.literal("§eOwner Right-Click§f: Open chest inventory to restock or collect payment."));
        player.sendSystemMessage(Component.literal("§eOwner Sneak + Right-Click§f: View buyer preview GUI."));
        player.sendSystemMessage(Component.literal("§6================================================="));
    }
}
