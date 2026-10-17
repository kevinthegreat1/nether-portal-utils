package com.kevinthegreat.netherportalutils.client

import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientChunkEvents
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.minecraft.client.Minecraft
import net.minecraft.client.multiplayer.ClientPacketListener

object NetherPortalUtilsClient : ClientModInitializer {
	@Volatile
    private var portalPreview: PortalPreview? = null

	override fun onInitializeClient() {
		ClientChunkEvents.CHUNK_LOAD.register { level, chunk ->
			val connection = Minecraft.getInstance().connection ?: return@register
			portalPreviewOf(connection).enqueue(level, chunk)
		}
		ClientTickEvents.END_CLIENT_TICK.register {
			val connection = it.connection
			val level = it.level
			val player = it.player

			if (connection == null || level == null || player == null) {
				portalPreview = null
				return@register
			}

			portalPreviewOf(connection).tick(level)
		}
		PortalPreviewRenderer.init()
	}

	fun portalPreviewOf(connection: ClientPacketListener) : PortalPreview {
		val current = portalPreview ?: PortalPreview(connection).also { portalPreview = it }
		if (current.connection == connection) {
			return current
		}

		return PortalPreview(connection).also { portalPreview = it }
	}
}
