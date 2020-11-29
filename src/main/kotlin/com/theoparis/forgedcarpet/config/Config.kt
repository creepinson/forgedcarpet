package com.theoparis.forgedcarpet.config

import net.minecraftforge.common.ForgeConfigSpec
import net.minecraftforge.common.ForgeConfigSpec.ConfigValue

object Config {
    var SPEC: ForgeConfigSpec? = null

    @JvmField
    var CONFIG: CarpetConfig? = null

    class CarpetConfig(builder: ForgeConfigSpec.Builder) {
        val pistonLimitVal: ConfigValue<Int> = builder
            .comment(" The piston block push limit")
            .translation("carpet.settings.pistonlimit")
            .define("pistonlimit", 12)
        val tickRateVal: ConfigValue<Float> = builder
            .comment(" The tick rate of the server")
            .translation("carpet.settings.tickrate")
            .define("tickrate", 20.0f)
        var pistonPushLimit: Int
            get() = pistonLimitVal.get()
            set(value) = pistonLimitVal.set(value)
        var tickRate: Float
            get() = tickRateVal.get()
            set(value) = tickRateVal.set(value)

    }

    init {
        val pair = ForgeConfigSpec.Builder().configure { builder: ForgeConfigSpec.Builder -> CarpetConfig(builder) }
        SPEC = pair.right
        CONFIG = pair.left
    }
}