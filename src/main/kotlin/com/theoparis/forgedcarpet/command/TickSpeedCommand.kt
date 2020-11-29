package com.theoparis.forgedcarpet.command

import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.FloatArgumentType
import com.mojang.brigadier.context.CommandContext
import com.theoparis.forgedcarpet.util.TickSpeed
import net.minecraft.command.CommandSource
import net.minecraft.command.Commands
import net.minecraft.command.arguments.MessageArgument
import net.minecraft.util.Util
import net.minecraft.util.text.ChatType
import net.minecraft.util.text.TranslationTextComponent

object TickRateCommand {
    fun register(dispatcher: CommandDispatcher<CommandSource?>) {
        dispatcher.register(
            Commands.literal("tickrate").requires { p_198627_0_: CommandSource ->
                p_198627_0_.hasPermissionLevel(
                    2
                )
            }.then(
                Commands.argument("rate", FloatArgumentType.floatArg())
                    .executes { p_198626_0_: CommandContext<CommandSource> ->
                        val rate = FloatArgumentType.getFloat(p_198626_0_, "rate")
                        TickSpeed.tickrate(rate, true)
                        1
                    })
        )
    }

}
