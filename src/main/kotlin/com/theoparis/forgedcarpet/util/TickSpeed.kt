package com.theoparis.forgedcarpet.util

import com.theoparis.forgedcarpet.ForgedCarpetMod
import net.minecraft.command.CommandSource
import net.minecraft.entity.player.ServerPlayerEntity
import net.minecraft.server.MinecraftServer
import net.minecraft.util.text.ITextComponent
import net.minecraft.util.text.StringTextComponent
import java.util.*
import java.util.function.BiConsumer

object TickSpeed {
    const val PLAYER_GRACE = 2

    @JvmField
    var tickRate = 20.0f

    @JvmField
    var mspt = 50.0f

    @JvmField
    var time_bias: Long = 0

    @JvmField
    var time_warp_start_time: Long = 0

    @JvmField
    var time_warp_scheduled_ticks: Long = 0

    @JvmField
    var time_advancerer: ServerPlayerEntity? = null

    @JvmField
    var tick_warp_callback: String? = null

    @JvmField
    var tick_warp_sender: CommandSource? = null

    @JvmField
    var player_active_timeout = 0

    @JvmField
    var process_entities = true

    @JvmField
    var deepFreeze = false

    @JvmField
    var is_paused = false

    @JvmField
    var isSuperhot = false

    /**
     * Functional interface that listens for tickrate changes. This is
     * implemented to allow tickrate compatibility with other mods etc.
     */
    private val tickrateListeners: MutableMap<String, BiConsumer<String?, Float>> = HashMap()
    private const val MIN_TICKRATE = 0.01f
    fun reset_player_active_timeout() {
        if (player_active_timeout < PLAYER_GRACE) {
            player_active_timeout = PLAYER_GRACE
        }
    }

    fun reset() {
        tickRate = 20.0f
        mspt = 50.0f
        time_bias = 0
        time_warp_start_time = 0
        time_warp_scheduled_ticks = 0
        time_advancerer = null
        tick_warp_callback = null
        tick_warp_sender = null
        player_active_timeout = 0
        process_entities = true
        deepFreeze = false
        is_paused = false
        isSuperhot = false
        notifyTickrateListeners("carpet")
    }

    fun add_ticks_to_run_in_pause(ticks: Int) {
        player_active_timeout = PLAYER_GRACE + ticks
    }

    fun tickrateAdvance(
        player: ServerPlayerEntity?,
        advance: Int,
        callback: String?,
        source: CommandSource
    ): ITextComponent {
        if (0 == advance) {
            tick_warp_callback = null
            if (source !== tick_warp_sender) tick_warp_sender = null
            finish_time_warp()
            tick_warp_sender = null
            return StringTextComponent("gi Warp interrupted")
        }
        if (time_bias > 0) {
            var who = "Another player"
            if (time_advancerer != null) who = time_advancerer!!.name.unformattedComponentText
            return StringTextComponent("l $who is already advancing time at the moment. Try later or ask them")
        }
        time_advancerer = player
        time_warp_start_time = System.nanoTime()
        time_warp_scheduled_ticks = advance.toLong()
        time_bias = advance.toLong()
        tick_warp_callback = callback
        tick_warp_sender = source
        return StringTextComponent("gi Warp speed ....")
    }

    fun finish_time_warp() {
        val completed_ticks = time_warp_scheduled_ticks - time_bias
        var milis_to_complete = System.nanoTime() - time_warp_start_time.toDouble()
        if (milis_to_complete == 0.0) {
            milis_to_complete = 1.0
        }
        milis_to_complete /= 1000000.0
        val tps = (1000.0 * completed_ticks / milis_to_complete).toInt()
        val mspt = 1.0 * milis_to_complete / completed_ticks
        time_warp_scheduled_ticks = 0
        time_warp_start_time = 0
        if (tick_warp_callback != null) {
            try {
                tick_warp_sender?.server?.commandManager?.handleCommand(tick_warp_sender, tick_warp_callback)
            } catch (var23: Throwable) {
                if (time_advancerer != null) {
                    Messenger.m(
                        time_advancerer!!.commandSource,
                        Messenger.compose(
                            "r Command Callback failed - unknown error: ",
                            "rb /$tick_warp_callback",
                            "/$tick_warp_callback"
                        )
                    )
                }
            }
            tick_warp_callback = null
            tick_warp_sender = null
        }
        if (time_advancerer != null) {
            Messenger.m(
                time_advancerer!!.commandSource,
                String.format("gi ... Time warp completed with %d tps, or %.2f mspt", tps, mspt)
            )
            time_advancerer = null
        } else {
            System.out.printf("... Time warp completed with %d tps, or %.2f mspt%n", tps, mspt)
        }
        time_bias = 0
    }

    fun continueWarp(): Boolean {
        return if (time_bias > 0) {
            if (time_bias == time_warp_scheduled_ticks) //first call after previous tick, adjust start time
            {
                time_warp_start_time = System.nanoTime()
            }
            time_bias -= 1
            true
        } else {
            finish_time_warp()
            false
        }
    }

    fun tick(server: MinecraftServer?) {
        process_entities = true
        if (player_active_timeout > 0) {
            player_active_timeout--
        }
        if (is_paused) {
            if (player_active_timeout < PLAYER_GRACE) {
                process_entities = false
            }
        } else if (isSuperhot) {
            if (player_active_timeout <= 0) {
                process_entities = false
            }
        }
    }

    //unused - mod compat reasons
    @JvmOverloads
    fun tickrate(rate: Float, update: Boolean = true) {
        tickRate = rate
        var mspt = (1000.0 / tickRate).toLong()
        if (mspt <= 0L) {
            mspt = 1L
            tickRate = 1000.0f
        }
        TickSpeed.mspt = mspt.toFloat()
        if (update) notifyTickrateListeners(ForgedCarpetMod.ID)
    }

    private fun tickRateChanged(modId: String, rate: Float) {
        // Other mods might change the tickrate in a slightly
        // different way. Also allow for tickrates that don't
        // divide into 1000 here.
        var rate = rate
        if (rate < MIN_TICKRATE) {
            rate = MIN_TICKRATE
        }
        tickRate = rate
        mspt = 1000.0f / tickRate
        notifyTickrateListeners(modId)
    }

    private fun notifyTickrateListeners(originModId: String?) {
        synchronized(tickrateListeners) {
            for ((key, value) in tickrateListeners) {
                if (originModId == null || originModId != key) {
                    value.accept(originModId, java.lang.Float.valueOf(tickRate))
                }
            }
        }
        ServerNetworkHandler.updateTickSpeedToConnectedPlayers()
    }

    fun addTickrateListener(modId: String, tickrateListener: BiConsumer<String?, Float>): BiConsumer<String, Float> {
        synchronized(tickrateListeners) { tickrateListeners.put(modId, tickrateListener) }
        return BiConsumer { modId: String, obj: Float -> tickRateChanged(modId, obj) }
    }
}