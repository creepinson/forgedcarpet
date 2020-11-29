package com.theoparis.forgedcarpet

import com.theoparis.forgedcarpet.command.CarpetCommand
import com.theoparis.forgedcarpet.config.Config
import net.minecraftforge.event.RegisterCommandsEvent
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.ModLoadingContext
import net.minecraftforge.fml.common.Mod
import net.minecraftforge.fml.config.ModConfig
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent
import org.apache.logging.log4j.Level
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import thedarkcolour.kotlinforforge.forge.FORGE_BUS
import thedarkcolour.kotlinforforge.forge.MOD_BUS

/**
 * Main mod class. Should be an `object` declaration annotated with `@Mod`.
 * The modid should be declared in this object and should match the modId entry
 * in mods.toml.
 *
 * An example for blocks is in the `blocks` package of this mod.
 */
@Mod(ForgedCarpetMod.ID)
object ForgedCarpetMod {
    // the modid of our mod
    const val ID: String = "forgedcarpet"

    // the logger for our mod
    val logger: Logger = LogManager.getLogger()

    init {
        logger.log(Level.INFO, "Hello world!")
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, Config.SPEC);

        // usage of the KotlinEventBus
        MOD_BUS.addListener(ForgedCarpetMod::onClientSetup)
        FORGE_BUS.addListener(EventHandler::onShift)
        FORGE_BUS.addListener(ForgedCarpetMod::registerCommands)
    }

    @SubscribeEvent
    fun registerCommands(event: RegisterCommandsEvent) {
        CarpetCommand.register(event.dispatcher)
    }

    /**
     * This is used for initializing client specific
     * things such as renderers and keymaps
     * Fired on the mod specific event bus.
     */
    private fun onClientSetup(event: FMLClientSetupEvent) {
        logger.log(Level.INFO, "Initializing client...")
    }
}