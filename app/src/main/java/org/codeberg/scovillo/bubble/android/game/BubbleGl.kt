package org.codeberg.scovillo.bubble.android.game

import org.codeberg.scovillo.bubble.game.BubbleMeshFactory
import org.codeberg.scovillo.bubble.game.GameObject
import org.codeberg.scovillo.bubble.game.GameObjectColor
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import javax.microedition.khronos.opengles.GL10
import kotlin.math.sin

fun GameObjectColor.rgb(): FloatArray = when (this) {
    GameObjectColor.RED -> floatArrayOf(0.76f, 0.08f, 0.18f, 1.0f)
    GameObjectColor.GREEN -> floatArrayOf(0.04f, 0.58f, 0.24f, 1.0f)
    GameObjectColor.SILVER -> floatArrayOf(0.62f, 0.70f, 0.80f, 1.0f)
    GameObjectColor.BLUE -> floatArrayOf(0.16f, 0.30f, 1.0f, 1.0f)
    GameObjectColor.PURPLE -> floatArrayOf(0.50f, 0.12f, 0.76f, 1.0f)
    GameObjectColor.GOLD -> floatArrayOf(0.82f, 0.52f, 0.035f, 1.0f)
}

class Bubble(private val state: GameObject) : OpenGlObject(state) {
    private val glColor = requireNotNull(state.color).rgb()

    val color: GameObjectColor get() = state.color
    val score: Int get() = state.score

    private val mesh = meshFor(12.0f)

    override fun draw(gl: GL10) {
        val popProgress = (state.disappearElapsedSeconds / POP_DURATION_SECONDS).coerceIn(0f, 1f)
        val wobbleX = 1f + .1f * sin(state.ageSeconds * 7f)
        val wobbleY = 1f + .1f * sin(state.ageSeconds * 7f + 1.7f)
        val bubbleScale = if (state.isDisappearing) 1f + 0.22f * popProgress else 1f
        val bubbleAlpha = if (state.isDisappearing) 1f - popProgress else 1f
        gl.glMatrixMode(GL10.GL_MODELVIEW)
        gl.glPushMatrix()
        run {
            gl.glMultMatrixf(transformationMatrix, 0)
            gl.glScalef(
                state.scale * wobbleX * bubbleScale,
                state.scale * bubbleScale,
                state.scale * wobbleY * bubbleScale
            )
            // A faint halo separates overlapping bubbles without changing hit areas.
            gl.glDisable(GL10.GL_LIGHTING)
            gl.glDepthMask(false)
            gl.glBlendFunc(GL10.GL_SRC_ALPHA, GL10.GL_ONE)
            val haloScale = if (state.isDisappearing) 1.16f + 0.55f * popProgress else 1.16f
            val haloAlpha = if (state.isDisappearing) 0.26f * (1f - popProgress) else 0.07f
            gl.glColor4f(glColor[0], glColor[1], glColor[2], haloAlpha)
            gl.glPushMatrix()
            gl.glScalef(haloScale, haloScale, haloScale)
            drawMesh(gl)
            gl.glPopMatrix()

            gl.glBlendFunc(GL10.GL_SRC_ALPHA, GL10.GL_ONE_MINUS_SRC_ALPHA)
            gl.glDepthMask(true)
            gl.glEnable(GL10.GL_LIGHTING)
            gl.glMaterialfv(
                GL10.GL_FRONT_AND_BACK,
                GL10.GL_AMBIENT_AND_DIFFUSE,
                floatArrayOf(glColor[0], glColor[1], glColor[2], bubbleAlpha),
                0
            )
            gl.glMaterialfv(GL10.GL_FRONT_AND_BACK, GL10.GL_SPECULAR, SPECULAR_COLOR, 0)
            gl.glMaterialf(GL10.GL_FRONT_AND_BACK, GL10.GL_SHININESS, 54.0f)
            drawMesh(gl)
            gl.glDisable(GL10.GL_LIGHTING)
            gl.glDepthMask(true)
        }
        gl.glPopMatrix()
    }

    private fun drawMesh(gl: GL10) {
        gl.glEnableClientState(GL10.GL_VERTEX_ARRAY)
        gl.glEnableClientState(GL10.GL_NORMAL_ARRAY)
        for (strip in mesh.strips) {
            strip.vertices.position(0)
            gl.glVertexPointer(3, GL10.GL_FLOAT, 0, strip.vertices)
            strip.normals.position(0)
            gl.glNormalPointer(GL10.GL_FLOAT, 0, strip.normals)
            gl.glDrawArrays(GL10.GL_TRIANGLE_STRIP, 0, strip.vertexCount)
        }
        gl.glDisableClientState(GL10.GL_VERTEX_ARRAY)
        gl.glDisableClientState(GL10.GL_NORMAL_ARRAY)
    }

    private data class VertexStrip(
        val vertices: FloatBuffer,
        val normals: FloatBuffer,
        val vertexCount: Int,
    )

    private data class BubbleMesh(val strips: List<VertexStrip>)

    companion object {
        private const val POP_DURATION_SECONDS = 0.22f
        private val SPECULAR_COLOR = floatArrayOf(0.68f, 0.72f, 0.84f, 1.0f)
        private val meshes = HashMap<Float, BubbleMesh>()

        private fun meshFor(smoothness: Float): BubbleMesh = synchronized(meshes) {
            meshes.getOrPut(smoothness) {
                BubbleMeshFactory.create(smoothness).map { data ->
                    VertexStrip(
                        vertices = bufferOf(data.vertices),
                        normals = bufferOf(requireNotNull(data.normals)),
                        vertexCount = data.vertexCount,
                    )
                }.let(::BubbleMesh)
            }
        }

        private fun bufferOf(values: FloatArray): FloatBuffer =
            ByteBuffer.allocateDirect(values.size * Float.SIZE_BYTES)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()
                .apply { put(values); position(0) }
    }

}
