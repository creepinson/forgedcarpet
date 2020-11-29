package com.theoparis.forgedcarpet.mixin

import com.theoparis.forgedcarpet.util.TickSpeed.reset_player_active_timeout
import net.minecraft.entity.player.ServerPlayerEntity
import net.minecraft.network.play.ServerPlayNetHandler
import net.minecraft.network.play.client.CInputPacket
import net.minecraft.network.play.client.CMoveVehiclePacket
import net.minecraft.network.play.server.SEntityPacket
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.Shadow
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo
import kotlin.math.abs

@Mixin(ServerPlayNetHandler::class)
class ServerPlayNetHandlerMixin {
    @Shadow
    var player: ServerPlayerEntity? = null

    @Shadow
    private val lastGoodX = 0.0

    @Shadow
    private val lastGoodY = 0.0

    @Shadow
    private val lastGoodZ = 0.0

    @Inject(method = ["processInput"], at = [At(value = "RETURN")])
    private fun checkMoves(p: CInputPacket, ci: CallbackInfo) {
        if (p.strafeSpeed != 0.0f || p.forwardSpeed != 0.0f || p.isJumping || p.isSneaking) {
            reset_player_active_timeout()
        }
    }

    @Inject(
        method = ["processVehicleMove"],
        at = [At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/network/ServerPlayerEntity;isSleeping()Z",
            shift = At.Shift.BEFORE
        )]
    )
    private fun processVehicleMove(p: CMoveVehiclePacket, ci: CallbackInfo) {
        val movedBy: Double = player!!.position.distanceSq(lastGoodX, lastGoodY, lastGoodZ, true)
        if (movedBy == 0.0) return
        // corrective tick
        if (movedBy < 0.0009 && lastMoved > 0.0009 && abs(
                player!!.getServer()!!.tickCounter - lastMovedTick - 20
            ) < 2
        )
            return

        if (movedBy > 0.0) {
            lastMoved = movedBy
            lastMovedTick = player!!.getServer()!!.tickCounter.toLong()
            reset_player_active_timeout()
        }
    }

    // to skip reposition adjustment check
    private var lastMovedTick = 0L
    private var lastMoved = 0.0
}