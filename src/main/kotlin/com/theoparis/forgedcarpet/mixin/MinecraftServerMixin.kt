package com.theoparis.forgedcarpet.mixin

import com.theoparis.forgedcarpet.ForgedCarpetMod
import com.theoparis.forgedcarpet.util.TickSpeed
import com.theoparis.forgedcarpet.util.TickSpeed.continueWarp
import net.minecraft.profiler.EmptyProfiler
import net.minecraft.profiler.IProfiler
import net.minecraft.profiler.LongTickDetector
import net.minecraft.server.MinecraftServer
import net.minecraft.util.Util
import net.minecraft.util.concurrent.RecursiveEventLoop
import net.minecraft.util.concurrent.TickDelayedTask
import net.minecraft.world.server.ServerWorld
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.Shadow
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo
import java.util.function.BooleanSupplier
import javax.annotation.Nullable
import kotlin.math.abs

@Mixin(MinecraftServer::class)
abstract class MinecraftServerMixin(name: String) : RecursiveEventLoop<TickDelayedTask?>(name) {
    @Shadow
    private var serverIsRunning = false

    @Shadow
    var serverTime: Long = 0

    @Shadow
    private val profiler: IProfiler = EmptyProfiler.INSTANCE

    @Shadow
    protected abstract fun tick(booleanSupplier_1: BooleanSupplier?)

    @Shadow
    private var timeOfLastWarning: Long = 0

    @get:Shadow
    abstract val worlds: Iterable<ServerWorld>
    private var carpetMsptAccum = 0.0f

    @Shadow
    private var runTasksUntil: Long = 0

    @Shadow
    private var isRunningScheduledTasks = false

    @Shadow
    protected abstract fun func_240773_a_(@Nullable p_240773_1_: LongTickDetector?)

    @Shadow
    protected abstract fun runScheduledTasks();

    init {
        println("mixin works")
    }

    // Replaced the above cancelled while statement with this one
    // could possibly just inject that mspt selection at the beginning of the loop, but then adding all mspt's to
    // replace 50L will be a hassle
    @Inject(method = ["func_240802_v_"], at = [At(value = "INVOKE", shift = At.Shift.AFTER)])
    private fun modifiedRunLoop(ci: CallbackInfo) {
        while (serverIsRunning) {
            //long long_1 = Util.getMeasuringTimeMs() - this.serverTime;
            //CM deciding on tick speed
            var msThisTick = 0L
            var long_1 = 0L
            if (TickSpeed.time_warp_start_time != 0L && continueWarp()) {
                //making sure server won't flop after the warp or if the warp is interrupted
                timeOfLastWarning = Util.milliTime()
                this.serverTime = timeOfLastWarning
                carpetMsptAccum = TickSpeed.mspt
            } else {
                if (abs(carpetMsptAccum - TickSpeed.mspt) > 1.0f) {
                    // Tickrate changed. Ensure that we use the correct value.
                    carpetMsptAccum = TickSpeed.mspt
                }
                msThisTick = carpetMsptAccum.toLong() // regular tick
                carpetMsptAccum += TickSpeed.mspt - msThisTick
                long_1 = Util.milliTime() - this.serverTime
            }
            //end tick deciding
            //smoothed out delay to include mcpt component. With 50L gives defaults.
            if (long_1 >  /*2000L*/1000L + 20 * TickSpeed.mspt && this.serverTime - timeOfLastWarning >=  /*15000L*/10000L + 100 * TickSpeed.mspt) {
                val long_2 = (long_1 / TickSpeed.mspt).toLong() //50L;
                ForgedCarpetMod.logger.warn(
                    "Can't keep up! Is the server overloaded? Running {}ms or {} ticks behind",
                    long_1,
                    long_2
                )
                this.serverTime += (long_2 * TickSpeed.mspt).toLong() //50L;
                timeOfLastWarning = this.serverTime
            }
            this.serverTime += msThisTick //50L;
            val longtickdetector: LongTickDetector? = LongTickDetector.func_233524_a_("Server")
            this.func_240773_a_(longtickdetector)

            profiler.startTick()
            profiler.startSection("tick")
            TickSpeed.tick(this as MinecraftServer);
            tick(if (TickSpeed.time_warp_start_time != 0L) BooleanSupplier { true } else BooleanSupplier {
                 super.isTaskRunning() || Util.milliTime() < if (isRunningScheduledTasks) runTasksUntil else serverTime
            })
            profiler.endStartSection("nextTickWait")
            if (TickSpeed.time_warp_start_time != 0L) // clearing all hanging tasks no matter what when warping
            {
                while (runEveryTask()) {
                    Thread.yield()
                }
            }
            this.isRunningScheduledTasks = true
            this.runTasksUntil = /*50L*/(Util.milliTime() +  /*50L*/msThisTick).coerceAtLeast(this.serverTime)
            // run all tasks (this will not do a lot when warping), but that's fine since we already run them
            this.runScheduledTasks()
            profiler.endSection()
            profiler.endTick()
            this.serverIsRunning = true
        }
    }

    private fun runEveryTask(): Boolean {
        return if (super.isTaskRunning()) {
            true
        } else {
            for (serverlevel in worlds)
                if (serverlevel.chunkProvider.driveOneTask())
                    return true
            false
        }
    }
}