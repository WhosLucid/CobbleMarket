package com.whoslucid.cobblemarket.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.whoslucid.cobblelib.api.PermissionApi;
import com.whoslucid.cobblelib.util.AdventureTranslator;
import com.whoslucid.cobblelib.util.PlayerUtils;
import com.whoslucid.cobblemarket.CobbleMarket;
import com.whoslucid.cobblemarket.ui.MarketMainMenu;
import com.whoslucid.cobblemarket.ui.PokemonListingsMenu;
import com.whoslucid.cobblemarket.ui.ItemListingsMenu;
import com.whoslucid.cobblemarket.ui.MyListingsMenu;
import com.whoslucid.cobblemarket.ui.ExpiredListingsMenu;
import com.whoslucid.cobblemarket.ui.HistoryMenu;
import com.whoslucid.cobblemarket.util.TimeUtils;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.server.level.ServerPlayer;

import java.util.concurrent.TimeUnit;

public class CommandTree {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        for (String literal : CobbleMarket.config.getCommands()) {
            var baseCommand = Commands.literal(literal)
                    .requires(source -> PermissionApi.hasPermission(source, "cobblemarket.base", 0));

            dispatcher.register(baseCommand
                    // /market - Open main menu
                    .executes(context -> {
                        if (context.getSource().isPlayer()) {
                            ServerPlayer player = context.getSource().getPlayerOrException();
                            MarketMainMenu.open(player);
                            return 1;
                        }
                        return 0;
                    })

                    // /market open - Same as base
                    .then(Commands.literal("open")
                            .executes(context -> {
                                if (context.getSource().isPlayer()) {
                                    ServerPlayer player = context.getSource().getPlayerOrException();
                                    MarketMainMenu.open(player);
                                    return 1;
                                }
                                return 0;
                            }))

                    // /market pokemon - Open Pokemon listings
                    .then(Commands.literal("pokemon")
                            .executes(context -> {
                                if (context.getSource().isPlayer()) {
                                    ServerPlayer player = context.getSource().getPlayerOrException();
                                    PokemonListingsMenu.open(player);
                                    return 1;
                                }
                                return 0;
                            }))

                    // /market items - Open Item listings
                    .then(Commands.literal("items")
                            .executes(context -> {
                                if (context.getSource().isPlayer()) {
                                    ServerPlayer player = context.getSource().getPlayerOrException();
                                    ItemListingsMenu.open(player);
                                    return 1;
                                }
                                return 0;
                            }))

                    // /market manage - Open player's listings
                    .then(Commands.literal("manage")
                            .executes(context -> {
                                if (context.getSource().isPlayer()) {
                                    ServerPlayer player = context.getSource().getPlayerOrException();
                                    MyListingsMenu.open(player);
                                    return 1;
                                }
                                return 0;
                            }))

                    // /market expired - Open expired listings
                    .then(Commands.literal("expired")
                            .executes(context -> {
                                if (context.getSource().isPlayer()) {
                                    ServerPlayer player = context.getSource().getPlayerOrException();
                                    ExpiredListingsMenu.open(player);
                                    return 1;
                                }
                                return 0;
                            }))

                    // /market history - Open transaction history
                    .then(Commands.literal("history")
                            .executes(context -> {
                                if (context.getSource().isPlayer()) {
                                    ServerPlayer player = context.getSource().getPlayerOrException();
                                    HistoryMenu.open(player);
                                    return 1;
                                }
                                return 0;
                            }))

                    // /market search <query> - Search listings
                    .then(Commands.literal("search")
                            .then(Commands.argument("query", StringArgumentType.greedyString())
                                    .executes(context -> {
                                        if (context.getSource().isPlayer()) {
                                            ServerPlayer player = context.getSource().getPlayerOrException();
                                            String query = StringArgumentType.getString(context, "query");
                                            PokemonListingsMenu.openWithSearch(player, query);
                                            return 1;
                                        }
                                        return 0;
                                    })))

                    // /market reload - Reload config (admin)
                    .then(Commands.literal("reload")
                            .requires(source -> PermissionApi.hasPermission(source, "cobblemarket.reload", 4))
                            .executes(context -> {
                                CobbleMarket.reload();
                                context.getSource().sendSuccess(() ->
                                        AdventureTranslator.toNative(CobbleMarket.language.getMessageReload()
                                                .replace("%prefix%", CobbleMarket.language.getPrefix())), true);
                                return 1;
                            }))

                    // /market admin timeout <player> <minutes>
                    .then(Commands.literal("admin")
                            .requires(source -> PermissionApi.hasPermission(source, "cobblemarket.admin", 4))
                            .then(Commands.literal("timeout")
                                    .then(Commands.argument("player", EntityArgument.player())
                                            .then(Commands.argument("minutes", IntegerArgumentType.integer(1))
                                                    .executes(context -> {
                                                        ServerPlayer target = EntityArgument.getPlayer(context, "player");
                                                        int minutes = IntegerArgumentType.getInteger(context, "minutes");

                                                        CobbleMarket.timeoutManager.addTimeout(
                                                                target.getUUID(),
                                                                TimeUnit.MINUTES.toMillis(minutes)
                                                        );

                                                        String message = CobbleMarket.language.getMessageTimeoutApplied()
                                                                .replace("%prefix%", CobbleMarket.language.getPrefix())
                                                                .replace("%player%", target.getName().getString())
                                                                .replace("%duration%", minutes + "m");

                                                        context.getSource().sendSuccess(() ->
                                                                AdventureTranslator.toNative(message), true);
                                                        return 1;
                                                    })))))

                    // /market admin remove <listingId>
                    .then(Commands.literal("admin")
                            .then(Commands.literal("remove")
                                    .then(Commands.argument("listingId", StringArgumentType.string())
                                            .executes(context -> {
                                                String idString = StringArgumentType.getString(context, "listingId");
                                                try {
                                                    java.util.UUID listingId = java.util.UUID.fromString(idString);
                                                    boolean removed = CobbleMarket.listingManager.removeListing(listingId);

                                                    if (removed) {
                                                        context.getSource().sendSuccess(() ->
                                                                AdventureTranslator.toNative(
                                                                        CobbleMarket.language.getMessageListingRemoved()
                                                                                .replace("%prefix%", CobbleMarket.language.getPrefix())), true);
                                                    } else {
                                                        context.getSource().sendFailure(
                                                                AdventureTranslator.toNative(
                                                                        CobbleMarket.language.getMessageListingNotFound()
                                                                                .replace("%prefix%", CobbleMarket.language.getPrefix())));
                                                    }
                                                    return removed ? 1 : 0;
                                                } catch (IllegalArgumentException e) {
                                                    context.getSource().sendFailure(
                                                            AdventureTranslator.toNative("&cInvalid listing ID format"));
                                                    return 0;
                                                }
                                            }))))
            );
        }
    }
}
