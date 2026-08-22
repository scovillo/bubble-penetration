package org.codeberg.scovillo.bubble.game
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import javax.microedition.khronos.opengles.GL10
import kotlin.math.cos
import kotlin.math.sin

enum class BubbleColors {
    RED, GREEN, LIGHTBLUE, BLUE, PURPLE
}

class Bubble(
    val color: BubbleColors?, private val glColor: FloatArray, speed: Float
) : GameObject(speed) {

    val score = 1

    private var wobbleX = 1.0f
    private var wobbleY = 0.7f

    private var wobbleXup = true
    private var wobbleYup = true
    private val mesh = meshFor(12.0f)

    override fun draw(gl: GL10) {
        gl.glMatrixMode(GL10.GL_MODELVIEW)
        gl.glPushMatrix()
        run {
            gl.glMultMatrixf(transformationMatrix, 0)
            gl.glScalef(scale * wobbleX, scale, scale * wobbleY)
            gl.glColor4f(glColor[0], glColor[1], glColor[2], glColor[3])
            gl.glEnableClientState(GL10.GL_VERTEX_ARRAY)
            gl.glEnableClientState(GL10.GL_NORMAL_ARRAY)
            for (strip in mesh.strips) {
                strip.vertices.position(0)
                gl.glVertexPointer(3, GL10.GL_FLOAT, 0, strip.vertices)
                gl.glNormalPointer(GL10.GL_FLOAT, 0, strip.vertices)
                gl.glDrawArrays(GL10.GL_TRIANGLE_STRIP, 0, strip.vertexCount)
            }
            gl.glDisableClientState(GL10.GL_VERTEX_ARRAY)
            gl.glDisableClientState(GL10.GL_NORMAL_ARRAY)
        }
        gl.glPopMatrix()
    }

    override fun update(fracSec: Float) {
        updatePosition(fracSec)

        if (wobbleX + 0.015 <= 1.1 && wobbleXup) {
            wobbleX += 0.015f
        } else {
            wobbleXup = false
        }
        if (wobbleX - 0.015 >= 0.9 && !wobbleXup) {
            wobbleX -= 0.015f
        } else {
            wobbleXup = true
        }
        if (wobbleY + 0.015 <= 1.1 && wobbleYup) {
            wobbleY += 0.015f
        } else {
            wobbleYup = false
        }
        if (wobbleY - 0.015 >= 0.9 && !wobbleYup) {
            wobbleY -= 0.015f
        } else {
            wobbleYup = true
        }
    }

    private data class VertexStrip(val vertices: FloatBuffer, val vertexCount: Int)

    private data class BubbleMesh(val strips: List<VertexStrip>)

    companion object {
        private val meshes = HashMap<Float, BubbleMesh>()

        private fun meshFor(smoothness: Float): BubbleMesh = synchronized(meshes) {
            meshes.getOrPut(smoothness) { createMesh(smoothness) }
        }

        private fun createMesh(smoothness: Float): BubbleMesh {
            val strips = mutableListOf<VertexStrip>()
            var angleA = -90.0f
            while (angleA < 90.0f) {
                val vertexCount = ((360.0f / smoothness).toInt() + 1) * 2
                val vertices = ByteBuffer.allocateDirect(vertexCount * 3 * Float.SIZE_BYTES)
                    .order(ByteOrder.nativeOrder())
                    .asFloatBuffer()
                val r1 = cos(angleA * Math.PI / 180.0).toFloat()
                val r2 = cos((angleA + smoothness) * Math.PI / 180.0).toFloat()
                val h1 = sin(angleA * Math.PI / 180.0).toFloat()
                val h2 = sin((angleA + smoothness) * Math.PI / 180.0).toFloat()
                var angleB = 0.0f
                while (angleB <= 360.0f) {
                    val cosine = cos(angleB * Math.PI / 180.0).toFloat()
                    val sine = (-sin(angleB * Math.PI / 180.0)).toFloat()
                    vertices.put(r2 * cosine).put(h2).put(r2 * sine)
                    vertices.put(r1 * cosine).put(h1).put(r1 * sine)
                    angleB += smoothness
                }
                strips.add(VertexStrip(vertices, vertexCount))
                angleA += smoothness
            }
            return BubbleMesh(strips)
        }
    }

}
