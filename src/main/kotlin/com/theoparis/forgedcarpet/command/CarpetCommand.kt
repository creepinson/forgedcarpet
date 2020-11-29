package com.theoparis.forgedcarpet.command

import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.FloatArgumentType
import com.mojang.brigadier.arguments.IntegerArgumentType
import com.mojang.brigadier.context.CommandContext
import com.theoparis.forgedcarpet.config.Config
import com.theoparis.forgedcarpet.util.Messenger
import com.theoparis.forgedcarpet.util.TickSpeed
import net.minecraft.command.CommandSource
import net.minecraft.command.Commands

object CarpetCommand {
    fun register(dispatcher: CommandDispatcher<CommandSource?>) {
        dispatcher.register(
            Commands.literal("carpet").then(
                Commands.literal("tickrate").requires { source: CommandSource ->
                    source.hasPermissionLevel(
                        2
                    )
                }.then(
                    Commands.argument("value", FloatArgumentType.floatArg())
                        .executes { ctx: CommandContext<CommandSource> ->
                            val value = FloatArgumentType.getFloat(ctx, "value")
                            TickSpeed.tickrate(value, true)
                            Config.CONFIG?.tickRate = TickSpeed.tickRate
                            Messenger.m(source = ctx.source, "Set tick rate to: $value")
                            1
                        })
            ).then(
                Commands.literal("pistonlimit").requires { source: CommandSource ->
                    source.hasPermissionLevel(
                        2
                    )
                }.then(
                    Commands.argument("value", IntegerArgumentType.integer())
                        .executes { ctx: CommandContext<CommandSource> ->
                            val value = IntegerArgumentType.getInteger(ctx, "value")
                            Config.CONFIG?.pistonPushLimit = value
                            Messenger.m(source = ctx.source, "Set piston push limit to: $value")
                            1
                        })
            )
        )
    }

}
