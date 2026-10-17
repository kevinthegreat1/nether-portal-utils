package com.kevinthegreat.netherportalutils.client

import com.kevinthegreat.netherportalutils.NetherPortalUtils
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.vertex.VertexConsumer
import com.mojang.renderpearl.api.buffers.GpuBufferSlice
import com.mojang.renderpearl.api.commands.RenderPass
import com.mojang.renderpearl.api.pipeline.PrimitiveTopology
import com.mojang.renderpearl.api.pipeline.RenderPipeline
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelExtractionContext
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelExtractionEvents
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.RenderPipelines
import net.minecraft.client.renderer.StagedVertexBuffer
import net.minecraft.client.renderer.rendertype.RenderType
import net.minecraft.client.renderer.state.level.CameraRenderState
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import org.joml.Matrix4f
import org.joml.Matrix4fc
import org.joml.Vector3f
import org.joml.Vector4f
import java.util.Optional

object PortalPreviewRenderer {
    private val pipeline = RenderPipelines.register(
        RenderPipeline.builder(RenderPipelines.DEBUG_FILLED_SNIPPET)
            .withLocation(NetherPortalUtils.id("pipeline/portal_preview"))
            .withDepthStencilState(Optional.empty())
            .build()
    )

    private val stagedBuffer = StagedVertexBuffer(
        { "Nether Portal Utils Portal Preview Buffer" },
        RenderType.SMALL_BUFFER_SIZE
    )

    private val colorModulator = Vector4f(1f, 1f, 1f, 1f)
    private val modelOffset = Vector3f()
    private val textureMatrix = Matrix4f()

    private var states: List<PortalRenderState> = emptyList()
    private var draw: StagedVertexBuffer.Draw? = null

    fun init() {
        LevelExtractionEvents.END_EXTRACTION.register(::extract)
        LevelRenderEvents.END_MAIN.register { stagedBuffer.endFrame() }
    }

    private fun extract(context: LevelExtractionContext) {
        val client = Minecraft.getInstance()
        val level = client.level
        val connection = client.connection

        states = if (level != null && connection != null) {
            NetherPortalUtilsClient.portalPreviewOf(connection)
                .previews(level.dimension())
                .map {
                    PortalRenderState(
                        pos = it.pos,
                        axis = it.axis
                    )
                }
        } else {
            emptyList()
        }
    }

    fun prepare(camera: CameraRenderState) {
        val formatBinding = pipeline.getVertexFormatBinding(0) ?: return
        val topology = pipeline.primitiveTopology
        val draw = stagedBuffer.appendDraw(
            formatBinding,
            topology,
            if (topology == PrimitiveTopology.QUADS) {
                RenderSystem.getProjectionType().vertexSorting()
            } else {
                null
            }
        )

        for (portal in states) {
            preparePortal(draw, portal, camera)
        }

        stagedBuffer.upload()
        this.draw = draw
    }

    private fun preparePortal(draw: StagedVertexBuffer.Draw, portal: PortalRenderState, camera: CameraRenderState) {
        val matrix = Matrix4f()
        val camera = camera.pos

        matrix.translate(-camera.x.toFloat(), -camera.y.toFloat(), -camera.z.toFloat())

        val vertices = stagedBuffer.getVertexBuilder(draw)

        val x = portal.pos.x.toFloat()
        val y = portal.pos.y.toFloat()
        val z = portal.pos.z.toFloat()
        val minX: Float
        val minZ: Float
        val maxX: Float
        val maxZ: Float

        if (portal.axis == Direction.Axis.X) {
            minX = x
            minZ = z + 0.375f
            maxX = x + 1
            maxZ = minZ + 0.25f
        } else {
            minX = x + 0.375f
            minZ = z
            maxX = minX + 0.25f
            maxZ = z + 1
        }

        prepareFilledBox(matrix, vertices, minX, y, minZ, maxX, y + 1, maxZ, 0.1f, 0.9f, 1f, 0.5f)
    }

    private fun prepareFilledBox(
        matrix: Matrix4fc, buffer: VertexConsumer,
        minX: Float, minY: Float, minZ: Float, maxX: Float, maxY: Float, maxZ: Float,
        red: Float, green: Float, blue: Float, alpha: Float
    ) {
        // Front
        buffer.addVertex(matrix, minX, minY, maxZ).setColor(red, green, blue, alpha)
        buffer.addVertex(matrix, maxX, minY, maxZ).setColor(red, green, blue, alpha)
        buffer.addVertex(matrix, maxX, maxY, maxZ).setColor(red, green, blue, alpha)
        buffer.addVertex(matrix, minX, maxY, maxZ).setColor(red, green, blue, alpha)

        // Back
        buffer.addVertex(matrix, maxX, minY, minZ).setColor(red, green, blue, alpha)
        buffer.addVertex(matrix, minX, minY, minZ).setColor(red, green, blue, alpha)
        buffer.addVertex(matrix, minX, maxY, minZ).setColor(red, green, blue, alpha)
        buffer.addVertex(matrix, maxX, maxY, minZ).setColor(red, green, blue, alpha)

        // Left
        buffer.addVertex(matrix, minX, minY, minZ).setColor(red, green, blue, alpha)
        buffer.addVertex(matrix, minX, minY, maxZ).setColor(red, green, blue, alpha)
        buffer.addVertex(matrix, minX, maxY, maxZ).setColor(red, green, blue, alpha)
        buffer.addVertex(matrix, minX, maxY, minZ).setColor(red, green, blue, alpha)

        // Right
        buffer.addVertex(matrix, maxX, minY, maxZ).setColor(red, green, blue, alpha)
        buffer.addVertex(matrix, maxX, minY, minZ).setColor(red, green, blue, alpha)
        buffer.addVertex(matrix, maxX, maxY, minZ).setColor(red, green, blue, alpha)
        buffer.addVertex(matrix, maxX, maxY, maxZ).setColor(red, green, blue, alpha)

        // Top
        buffer.addVertex(matrix, minX, maxY, maxZ).setColor(red, green, blue, alpha)
        buffer.addVertex(matrix, maxX, maxY, maxZ).setColor(red, green, blue, alpha)
        buffer.addVertex(matrix, maxX, maxY, minZ).setColor(red, green, blue, alpha)
        buffer.addVertex(matrix, minX, maxY, minZ).setColor(red, green, blue, alpha)

        // Bottom
        buffer.addVertex(matrix, minX, minY, minZ).setColor(red, green, blue, alpha)
        buffer.addVertex(matrix, maxX, minY, minZ).setColor(red, green, blue, alpha)
        buffer.addVertex(matrix, maxX, minY, maxZ).setColor(red, green, blue, alpha)
        buffer.addVertex(matrix, minX, minY, maxZ).setColor(red, green, blue, alpha)
    }

    fun executeDraw(pass: RenderPass) {
        val info = stagedBuffer.getExecuteInfo(draw ?: return)
        if (info != null) {
            executeDraw(info, pass)
        }
    }

    private fun executeDraw(info: StagedVertexBuffer.ExecuteInfo, pass: RenderPass) {
        val dynamicTransforms: GpuBufferSlice = RenderSystem.getDynamicUniforms().writeTransform(
            RenderSystem.getModelViewMatrixCopy(),
            colorModulator,
            modelOffset,
            textureMatrix
        )

        pass.pushDebugGroup { "Nether Portal Utils Portal Preview" }
        pass.setPipeline(RenderSystem.getCompiledPipeline(pipeline))
        RenderSystem.bindDefaultUniforms(pass)
        pass.setUniform("DynamicTransforms", dynamicTransforms)
        pass.setVertexBuffer(0, info.vertexBuffer().slice())
        pass.setIndexBuffer(info.indexBuffer(), info.indexType())
        pass.drawIndexed(info.indexCount(), 1, info.firstIndex(), info.baseVertex(), 0)
        pass.popDebugGroup()
    }

    fun close() {
        states = emptyList()
        draw = null
        stagedBuffer.close()
    }

    data class PortalRenderState(
        val pos: BlockPos,
        val axis: Direction.Axis,
        val red: Float = 0.1f,
        val green: Float = 0.9f,
        val blue: Float = 1.0f,
        val alpha: Float = 0.65f
    )
}
