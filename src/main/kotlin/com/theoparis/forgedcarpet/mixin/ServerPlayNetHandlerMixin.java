package com.theoparis.forgedcarpet.mixin;

import com.theoparis.forgedcarpet.util.TickSpeed;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.play.ServerPlayNetHandler;
import net.minecraft.network.play.client.CInputPacket;
import net.minecraft.network.play.client.CMoveVehiclePacket;
import net.minecraft.network.play.server.SEntityPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.Nullable;
import java.util.Objects;

@Mixin(ServerPlayNetHandler.class)
class ServerPlayNetHandlerMixin {
    @Shadow
    @Nullable
    public ServerPlayerEntity player;

    @Shadow
    private double lastGoodX = 0.0;

    @Shadow
    private double lastGoodY = 0.0;

    @Shadow
    private double lastGoodZ = 0.0;

    @Inject(method = "processInput", at = @At(value = "RETURN"))
    private void checkMoves(CInputPacket p, CallbackInfo ci) {
        if (p.getStrafeSpeed() != 0.0f || p.getForwardSpeed() != 0.0f || p.isJumping() || p.isSneaking()) {
            TickSpeed.INSTANCE.reset_player_active_timeout();
        }
    }

    @Inject(
            method = "processVehicleMove",
            at = @At(
                    value = "INVOKE",
                    shift = At.Shift.BEFORE
            )
    )
    private void processVehicleMove(CMoveVehiclePacket p, CallbackInfo ci) {
        double movedBy = Objects.requireNonNull(player).getPosition().distanceSq(lastGoodX, lastGoodY, lastGoodZ, true);
        if (movedBy == 0.0) return;
        // corrective tick
        if (movedBy < 0.0009 && lastMoved > 0.0009 && Math.abs(
                Objects.requireNonNull(player.getServer()).getTickCounter() - lastMovedTick - 20
        ) < 2)
            return;

        if (movedBy > 0.0) {
            lastMoved = movedBy;
            lastMovedTick = Objects.requireNonNull(player.getServer()).getTickCounter();
            TickSpeed.INSTANCE.reset_player_active_timeout();
        }
    }

    // to skip reposition adjustment check
    private static long lastMovedTick = 0L;
    private static double lastMoved = 0.0;
}