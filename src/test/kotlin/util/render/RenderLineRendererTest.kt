package moe.nea.firmament.test.util.render

import com.mojang.blaze3d.PrimitiveTopology
import com.mojang.blaze3d.vertex.BufferBuilder
import com.mojang.blaze3d.vertex.ByteBufferBuilder
import com.mojang.blaze3d.vertex.DefaultVertexFormat
import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.blaze3d.vertex.VertexConsumer
import java.lang.reflect.InvocationTargetException
import java.nio.ByteOrder
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import net.minecraft.client.gui.navigation.ScreenRectangle
import net.minecraft.util.ARGB
import moe.nea.firmament.util.render.gui.pip.RenderLineRenderer
import moe.nea.firmament.util.render.state.gui.RenderLineState

class RenderLineRendererTest {
	@Test
	fun slotBindingLinesProduceCompleteColoredVertices() {
		// Exercise the submitted geometry without creating a GPU renderer or window.
		// The callback is a compiler-generated method, so its name follows the production lambda.
		val renderGeometry = RenderLineRenderer::class.java.getDeclaredMethod(
			"lambda\$renderToTexture\$0",
			RenderLineState::class.java, PoseStack.Pose::class.java, VertexConsumer::class.java,
		).apply { isAccessible = true }
		val format = DefaultVertexFormat.POSITION_COLOR_NORMAL_LINE_WIDTH
		val color = 0xFF12AB34.toInt()
		for (direction in RenderLineState.LineDirection.entries) {
			val state = RenderLineState(
				0, 0, 40, 20, 1f, ScreenRectangle(0, 0, 40, 20), 2f, 40, 20, color, direction,
			)
			ByteBufferBuilder(256).use { storage ->
				val buffer = BufferBuilder(storage, PrimitiveTopology.LINES, format)
				try {
					renderGeometry.invoke(null, state, PoseStack().last(), buffer)
				} catch (error: InvocationTargetException) {
					throw error.cause!!
				}
				buffer.buildOrThrow().use { mesh ->
					assertEquals(4, mesh.drawState().vertexCount())
					val vertices = mesh.vertexBuffer().order(ByteOrder.LITTLE_ENDIAN)
					for (vertex in 0 until mesh.drawState().vertexCount()) {
						val colorOffset = vertex * format.vertexSize + format.getElement("Color")!!.offset()
						assertEquals(ARGB.toABGR(color), vertices.getInt(colorOffset))
					}
				}
			}
		}
	}
}
