package com.theoparis.forgedcarpet.util

import com.theoparis.forgedcarpet.ForgedCarpetMod
import com.theoparis.forgedcarpet.ForgedCarpetMod.logger
import com.theoparis.forgedcarpet.util.TickSpeed.tickRate
import io.netty.buffer.Unpooled
import net.minecraft.command.CommandSource
import net.minecraft.entity.player.ServerPlayerEntity
import net.minecraft.nbt.CompoundNBT
import net.minecraft.nbt.INBT
import net.minecraft.nbt.ListNBT
import net.minecraft.nbt.StringNBT
import net.minecraft.network.PacketBuffer
import net.minecraft.network.play.client.CCustomPayloadPacket
import net.minecraft.util.ResourceLocation
import net.minecraft.util.text.ITextComponent
import net.minecraft.world.server.ServerWorld
import java.util.*
import java.util.function.BiConsumer

object ServerNetworkHandler {
    private val remoteCarpetPlayers: MutableMap<ServerPlayerEntity, String> = HashMap()
    private val validCarpetPlayers: MutableSet<ServerPlayerEntity> = HashSet()
    val CARPET_CHANNEL = ResourceLocation(ForgedCarpetMod.ID, "hello")
    const val HI = 69
    const val HELLO = 420
    const val DATA = 1
    private val dataHandlers: Map<String, BiConsumer<ServerPlayerEntity, INBT?>> =
        object : HashMap<String, BiConsumer<ServerPlayerEntity, INBT?>>() {
            init {
                put(
                    "clientCommand",
                    BiConsumer { p: ServerPlayerEntity, t: INBT? -> handleClientCommand(p, t as CompoundNBT) })
            }
        }

    fun handleData(data: PacketBuffer?, player: ServerPlayerEntity) {
        if (data != null) {
            val id = data.readVarInt()
            if (id == DATA) onClientData(player, data)
        }
    }

    private fun handleClientCommand(player: ServerPlayerEntity, commandData: CompoundNBT) {
        val command = commandData.getString("command")
        val id = commandData.getString("id")
        val output: MutableList<ITextComponent> = ArrayList()
        val error = arrayOf("")
        var resultCode = -1
        if (player.getServer() == null) {
            error[0] = "No Server"
        } else {
            resultCode = player.getServer()!!.commandManager.handleCommand(
                object : CommandSource(
                    player, player.positionVec, player.commandSource.rotation,
                    if (player.world is ServerWorld) player.world as ServerWorld else null,
                    player.server.getPermissionLevel(player.gameProfile), player.name.string, player.displayName,
                    Objects.requireNonNull(player.world.server), player
                ) {
                    override fun sendErrorMessage(message: ITextComponent) {
                        error[0] = message.string
                    }

                    override fun sendFeedback(message: ITextComponent, broadcastToOps: Boolean) {
                        output.add(message)
                    }
                },
                command
            )
        }
        val result = CompoundNBT()
        result.putString("id", id)
        result.putInt("code", resultCode)
        if (error[0] != null) result.putString("error", error[0])
        val outputResult = ListNBT()
        for (line in output) outputResult.add(StringNBT.valueOf(ITextComponent.Serializer.toJson(line)))
        if (output.isNotEmpty()) result.put("output", outputResult)
        player.connection.sendPacket(
            CCustomPayloadPacket(
                CARPET_CHANNEL,
                DataBuilder.create().withCustomNBT("clientCommand", result).build()
            )
        )
        // run command plug to command output,
    }

    private fun onClientData(player: ServerPlayerEntity, data: PacketBuffer) {
        val compound = data.readCompoundTag() ?: return
        for (key in compound.keySet()) {
            if (dataHandlers.containsKey(key)) dataHandlers[key]!!
                .accept(player, compound[key]) else logger.warn("Unknown carpet client data: $key")
        }
    }

    fun updateTickSpeedToConnectedPlayers() {
        for (player in remoteCarpetPlayers.keys) {
            player.connection.sendPacket(
                CCustomPayloadPacket(
                    CARPET_CHANNEL,
                    DataBuilder.create().withTickRate().build()
                )
            )
        }
    }

    fun broadcastCustomCommand(command: String?, data: INBT?) {
        for (player in validCarpetPlayers) {
            player.connection.sendPacket(
                CCustomPayloadPacket(
                    CARPET_CHANNEL,
                    DataBuilder.create().withCustomNBT(command, data).build()
                )
            )
        }
    }

    fun sendCustomCommand(player: ServerPlayerEntity, command: String?, data: INBT?) {
        if (isValidCarpetPlayer(player)) {
            player.connection.sendPacket(
                CCustomPayloadPacket(
                    CARPET_CHANNEL,
                    DataBuilder.create().withCustomNBT(command, data).build()
                )
            )
        }
    }

    fun onPlayerLoggedOut(player: ServerPlayerEntity) {
        validCarpetPlayers.remove(player)
        if (!player.connection.netManager.isLocalChannel) remoteCarpetPlayers.remove(player)
    }

    fun close() {
        remoteCarpetPlayers.clear()
        validCarpetPlayers.clear()
    }

    fun isValidCarpetPlayer(player: ServerPlayerEntity): Boolean {
        return validCarpetPlayers.contains(player)
    }

    private class DataBuilder private constructor() {
        private val INBT: CompoundNBT
        fun withTickRate(): DataBuilder {
            INBT.putFloat("TickRate", tickRate)
            return this
        }

        fun withCustomNBT(key: String?, value: INBT?): DataBuilder {
            INBT.put(key, value)
            return this
        }

        fun build(): PacketBuffer {
            val packetBuf = PacketBuffer(Unpooled.buffer())
            packetBuf.writeVarInt(DATA)
            packetBuf.writeCompoundTag(INBT)
            return packetBuf
        }

        companion object {
            fun create(): DataBuilder {
                return DataBuilder()
            }
        }

        init {
            INBT = CompoundNBT()
        }
    }
}