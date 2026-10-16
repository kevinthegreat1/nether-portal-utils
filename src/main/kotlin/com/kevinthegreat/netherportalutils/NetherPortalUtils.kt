package com.kevinthegreat.netherportalutils

import net.fabricmc.api.ModInitializer
import net.minecraft.resources.Identifier
import org.slf4j.LoggerFactory

object NetherPortalUtils : ModInitializer {
	const val MOD_ID: String = "nether-portal-utils"

	private val LOGGER = LoggerFactory.getLogger(MOD_ID)

	override fun onInitialize() {}

	fun id(path: String): Identifier = Identifier.fromNamespaceAndPath(MOD_ID, path)
}
