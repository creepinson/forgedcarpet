package com.theoparis.forgedcarpet.util

import net.minecraft.command.CommandSource
import net.minecraft.util.text.ITextComponent
import net.minecraft.util.text.StringTextComponent
import net.minecraft.util.text.event.ClickEvent
import net.minecraft.util.text.event.HoverEvent
import net.minecraft.world.World


object Messenger {
    // message source
    fun m(source: CommandSource?, msg: ITextComponent) {
        source?.sendFeedback(
            msg,
            source.server.getWorld(World.OVERWORLD) != null
        )
    }

    fun m(source: CommandSource?, msg: String) {
        source?.sendFeedback(
            StringTextComponent(msg),
            source.server.getWorld(World.OVERWORLD) != null
        )
    }

    /**
     * composes single line, multicomponent message, and returns as one chat messagge
     */
    fun compose(vararg fields: Any): StringTextComponent {
        val message = StringTextComponent("")
        var previousComponent: ITextComponent? = null
        for (o in fields) {
            if (o is ITextComponent) {
                message.append(o)
                previousComponent = o
                continue
            }
            val txt = o.toString()
            val comp: ITextComponent = getComponentFromDesc(txt, previousComponent) ?: StringTextComponent("")
            if (comp !== previousComponent) message.append(comp)
            previousComponent = comp
        }
        return message
    }

    private fun getComponentFromDesc(message: String, previousMsg: ITextComponent?): ITextComponent? {
        var msg = message
        if (msg.equals("", ignoreCase = true)) {
            return StringTextComponent("")
        }
        if (Character.isWhitespace(msg[0])) {
            msg = "w$msg"
        }
        val limit = msg.indexOf(' ')
        var desc = msg
        var str = ""
        if (limit >= 0) {
            desc = msg.substring(0, limit)
            str = msg.substring(limit + 1)
        }
        if (desc[0] == '/') // deprecated
        {
            if (previousMsg != null) previousMsg.style.clickEvent = ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, msg)

            return previousMsg
        }
        if (desc[0] == '?') {
            if (previousMsg != null) previousMsg.style.clickEvent =
                ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, msg.substring(1))

            return previousMsg
        }
        if (desc[0] == '!') {
            if (previousMsg != null) previousMsg.style.clickEvent =
                ClickEvent(ClickEvent.Action.RUN_COMMAND, msg.substring(1))

            return previousMsg
        }
        if (desc[0] == '^') {
            previousMsg?.style?.hoverEvent = HoverEvent(HoverEvent.Action.SHOW_TEXT, compose(msg.substring(1)))

            return previousMsg
        }
        return StringTextComponent(str)
    }
}