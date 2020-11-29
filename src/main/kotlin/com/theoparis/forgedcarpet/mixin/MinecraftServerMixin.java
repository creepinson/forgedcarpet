package com.theoparis.forgedcarpet.mixin;

import com.theoparis.forgedcarpet.ForgedCarpetMod;
import com.theoparis.forgedcarpet.util.TickSpeed;
import net.minecraft.profiler.EmptyProfiler;
import net.minecraft.profiler.IProfiler;
import net.minecraft.profiler.LongTickDetector;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.RegistryKey;
import net.minecraft.util.Util;
import net.minecraft.util.concurrent.RecursiveEventLoop;
import net.minecraft.util.concurrent.TickDelayedTask;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;
import java.util.function.BooleanSupplier;
import javax.annotation.Nullable;

@Mixin(MinecraftServer.class)
abstract class MinecraftServerMixin extends RecursiveEventLoop<TickDelayedTask> {
    public MinecraftServerMixin(String name) {
        super(name);
        ForgedCarpetMod.INSTANCE.getLogger().info("mixin works");
    }

    @Shadow
    private volatile boolean serverIsRunning = false;

    @Shadow
    protected long serverTime = 0L;

    @Shadow
    private IProfiler profiler = EmptyProfiler.INSTANCE;

    @Shadow
    protected abstract void tick(BooleanSupplier supplier);

    @Shadow
    private long timeOfLastWarning = 0L;

    @Final
    @Shadow
    private Map<RegistryKey<World>, ServerWorld> worlds;


    @Shadow
    private long runTasksUntil = 0L;

    @Shadow
    private boolean isRunningScheduledTasks = false;

    @Shadow
    protected abstract void func_240773_a_(@Nullable LongTickDetector p_240773_1_);

    @Shadow
    protected abstract void runScheduledTasks();

    private float carpetMsptAccum = 0.0f;


    // Replaced the above cancelled while statement with this one
// could possibly just inject that mspt selection at the beginning of the loop, but then adding all mspt's to
// replace 50L will be a hassle
    @Inject(method = "func_240802_v_", at = @At(value = "INVOKE", shift = At.Shift.AFTER))
    private void modifiedRunLoop(CallbackInfo ci) {
        while (serverIsRunning) {
            //long long_1 = Util.getMeasuringTimeMs() - this.serverTime;
            //CM deciding on tick speed
            long msThisTick = 0L;
            long long_1 = 0L;
            if (TickSpeed.time_warp_start_time != 0L && TickSpeed.INSTANCE.continueWarp()) {
                //making sure server won't flop after the warp or if the warp is interrupted
                timeOfLastWarning = Util.milliTime();
                this.serverTime = timeOfLastWarning;
                carpetMsptAccum = TickSpeed.mspt;
            } else {
                if (Math.abs(carpetMsptAccum - TickSpeed.mspt) > 1.0f) {
                    // Tickrate changed. Ensure that we use the correct value.
                    carpetMsptAccum = TickSpeed.mspt;
                }
                msThisTick = (long) carpetMsptAccum; // regular tick
                carpetMsptAccum += TickSpeed.mspt - msThisTick;
                long_1 = Util.milliTime() - this.serverTime;
            }
            //end tick deciding
            //smoothed out delay to include mcpt component. With 50L gives defaults.
            if (long_1 >  /*2000L*/1000L + 20 * TickSpeed.mspt && this.serverTime - timeOfLastWarning >=  /*15000L*/10000L + 100 * TickSpeed.mspt) {
                long long_2 = (long) (long_1 / TickSpeed.mspt); //50L;
                ForgedCarpetMod.INSTANCE.getLogger().warn(
                        "Can't keep up! Is the server overloaded? Running {}ms or {} ticks behind",
                        long_1,
                        long_2
                );
                this.serverTime += (long_2 * TickSpeed.mspt); //50L;
                timeOfLastWarning = this.serverTime;
            }
            this.serverTime += msThisTick; //50L;
            LongTickDetector longtickdetector = LongTickDetector.func_233524_a_("Server");
            this.func_240773_a_(longtickdetector);

            profiler.startTick();
            profiler.startSection("tick");
            TickSpeed.INSTANCE.tick((MinecraftServer) (Object) this);
            boolean shouldTick;

            tick(() -> {
                if (TickSpeed.time_warp_start_time != 0L)
                    return true;
                else
                    return super.isTaskRunning() || Util.milliTime() < (isRunningScheduledTasks ? runTasksUntil : serverTime);
            });
            profiler.endStartSection("nextTickWait");
            if (TickSpeed.time_warp_start_time != 0L) // clearing all hanging tasks no matter what when warping
            {
                while (runEveryTask()) {
                    Thread.yield();
                }
            }
            this.isRunningScheduledTasks = true;
            this.runTasksUntil = /*50L*/Math.max((Util.milliTime() +  /*50L*/msThisTick), this.serverTime);
            // run all tasks (this will not do a lot when warping), but that's fine since we already run them
            this.runScheduledTasks();
            profiler.endSection();
            profiler.endTick();
            this.serverIsRunning = true;
        }
    }

    private boolean runEveryTask() {
        if (super.isTaskRunning())
            return true;
        else {
            for (ServerWorld serverlevel : worlds.values())
                return serverlevel.getChunkProvider().driveOneTask();
            return false;
        }
    }
}