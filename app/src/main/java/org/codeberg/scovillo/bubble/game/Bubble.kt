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

    val bubbleSmoothness = 12.0f
    private var wobbleX = 1.0f
    private var wobbleY = 0.7f

    private var wobbleXup = true
    private var wobbleYup = true

    override fun draw(gl: GL10) {
        gl.glMatrixMode(GL10.GL_MODELVIEW)
        gl.glPushMatrix()
        run {
            gl.glMultMatrixf(transformationMatrix, 0)
            gl.glScalef(scale * wobbleX, scale, scale * wobbleY)
            gl.glColor4f(glColor[0], glColor[1], glColor[2], glColor[3])
            var angleB: Float
            var cos: Float
            var sin: Float
            var r1: Float
            var r2: Float
            var h1: Float
            var h2: Float
            val v = Array(32) { FloatArray(3) }
            val vBuf: FloatBuffer
            val vbb: ByteBuffer = ByteBuffer.allocateDirect(v.size * v[0].size * 4)
            vbb.order(ByteOrder.nativeOrder())
            vBuf = vbb.asFloatBuffer()
            gl.glEnableClientState(GL10.GL_VERTEX_ARRAY)
            gl.glEnableClientState(GL10.GL_NORMAL_ARRAY)
            var angleA: Float = -90.0f
            while (angleA < 90.0f) {
                var n = 0
                r1 = cos(angleA * Math.PI / 180.0).toFloat()
                r2 = cos((angleA + bubbleSmoothness) * Math.PI / 180.0).toFloat()
                h1 = sin(angleA * Math.PI / 180.0).toFloat()
                h2 = sin((angleA + bubbleSmoothness) * Math.PI / 180.0).toFloat()

                angleB = 0.0f
                while (angleB <= 360.0f) {
                    cos = cos(angleB * Math.PI / 180.0).toFloat()
                    sin = (-sin(angleB * Math.PI / 180.0)).toFloat()
                    v[n][0] = r2 * cos
                    v[n][1] = h2
                    v[n][2] = r2 * sin
                    v[n + 1][0] = r1 * cos
                    v[n + 1][1] = h1
                    v[n + 1][2] = r1 * sin
                    vBuf.put(v[n])
                    vBuf.put(v[n + 1])
                    n += 2
                    if (n > 31) {
                        vBuf.position(0)
                        gl.glVertexPointer(3, GL10.GL_FLOAT, 0, vBuf)
                        gl.glNormalPointer(GL10.GL_FLOAT, 0, vBuf)
                        gl.glDrawArrays(GL10.GL_TRIANGLE_STRIP, 0, n)
                        n = 0
                        angleB -= bubbleSmoothness
                    }
                    angleB += bubbleSmoothness
                }
                vBuf.position(0)
                gl.glVertexPointer(3, GL10.GL_FLOAT, 0, vBuf)
                gl.glNormalPointer(GL10.GL_FLOAT, 0, vBuf)
                gl.glDrawArrays(GL10.GL_TRIANGLE_STRIP, 0, n)
                angleA += bubbleSmoothness
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

}
