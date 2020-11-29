package com.theoparis.forgedcarpet

import com.theoparis.forgedcarpet.util.TickSpeed
import net.minecraft.client.Minecraft
import net.minecraftforge.event.TickEvent
import net.minecraftforge.eventbus.api.SubscribeEvent
import sun.audio.AudioPlayer.player

class EventHandler {
    companion object {
        @SubscribeEvent
        fun onShift(event: TickEvent.ClientTickEvent) {
            if (event.phase === TickEvent.Phase.END) {
                val mc = Minecraft.getInstance()
/*                if (mc.gameSettings.keyBindSneak.isPressed) {
                    println("test ${TickSpeed.tickRate}")
                    TickSpeed.tickrate(TickSpeed.tickRate - 1, true)
                }*/
            }
        }
    }
}
