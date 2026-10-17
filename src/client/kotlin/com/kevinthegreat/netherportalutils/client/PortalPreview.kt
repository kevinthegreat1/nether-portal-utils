package com.kevinthegreat.netherportalutils.client

import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.client.multiplayer.ClientPacketListener
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.resources.ResourceKey
import net.minecraft.world.level.ChunkPos
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.NetherPortalBlock
import net.minecraft.world.level.chunk.LevelChunk
import kotlin.math.floor

class PortalPreview(val connection: ClientPacketListener) {
    private val dimensions = mutableMapOf<ResourceKey<Level>, DimensionData>()

    private fun dataOf(dimension: ResourceKey<Level>): DimensionData = dimensions.getOrPut(dimension) { DimensionData() }

    fun enqueue(level: ClientLevel, chunk: LevelChunk) = dataOf(level.dimension()).enqueue(chunk.pos)

    fun tick(level: ClientLevel) = dataOf(level.dimension()).tick(level)

    fun previews(targetDimension: ResourceKey<Level>): List<Portal> {
        val sourceDimension = when (targetDimension) {
            Level.OVERWORLD -> Level.NETHER
            Level.NETHER -> Level.OVERWORLD
            else -> return emptyList()
        }

        return dataOf(sourceDimension).previews(targetDimension)
    }

    private data class DimensionData(
        val portals: MutableSet<Portal> = mutableSetOf<Portal>(),
        val pendingChunks: MutableSet<ChunkPos> = mutableSetOf(),
        val scannedChunks: MutableSet<ChunkPos> = mutableSetOf()
    ) {
        fun enqueue(pos: ChunkPos) {
            if (pos !in scannedChunks) {
                pendingChunks.add(pos)
            }
        }

        fun tick(level: ClientLevel) {
            val pos = pendingChunks.firstOrNull() ?: return
            pendingChunks.remove(pos)
            if (!scannedChunks.add(pos)) return

            if (!level.chunkSource.hasChunk(pos.x, pos.z)) return
            scanChunk(level.getChunk(pos.x, pos.z))
        }

        private fun scanChunk(chunk: LevelChunk) {
            val portals = mutableSetOf<Portal>()

            val minX = chunk.pos.minBlockX
            val minZ = chunk.pos.minBlockZ
            val minY = chunk.minY
            val maxY = chunk.maxY

            val pos = BlockPos.MutableBlockPos()
            for (x in minX until minX + 16) {
                for (z in minZ until minZ + 16) {
                    for (y in minY until maxY) {
                        pos.set(x, y, z)
                        val state = chunk.getBlockState(pos)

                        if (!state.`is`(Blocks.NETHER_PORTAL) || !state.hasProperty(NetherPortalBlock.AXIS)) continue

                        portals.add(Portal(
                            pos.immutable(),
                            state.getValue(NetherPortalBlock.AXIS)
                        ))
                    }
                }
            }

            this.portals.addAll(portals)
        }

        fun previews(targetDimension: ResourceKey<Level>): List<Portal> {
            return portals
                .mapNotNull { it.project(targetDimension) }
                .distinct()
        }
    }

    data class Portal(
        val pos: BlockPos,
        val axis: Direction.Axis
    ) {
        fun project(targetDimension: ResourceKey<Level>): Portal? {
            val scale = when (targetDimension) {
                Level.NETHER -> 1.0 / 8.0
                Level.OVERWORLD -> 8.0
                else -> return null
            }

            return Portal(
                pos = BlockPos(
                    floor(pos.x * scale).toInt(),
                    pos.y,
                    floor(pos.z * scale).toInt()
                ),
                axis = axis
            )
        }
    }
}
