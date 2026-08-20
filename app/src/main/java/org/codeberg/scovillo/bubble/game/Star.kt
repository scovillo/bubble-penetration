package org.codeberg.scovillo.bubble.game

import android.opengl.Matrix
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.ShortBuffer
import javax.microedition.khronos.opengles.GL10

class Star(speed: Float) : GameObject(speed) {

    val score = 3

    private var rotation = 0.0f
    private val angularVelocity = 50 + Math.random().toFloat() * 100
    private val rotationAxis = floatArrayOf(0.0f, 1.0f, 0.0f)

    override fun draw(gl: GL10) {
        gl.glMatrixMode(GL10.GL_MODELVIEW)
        gl.glPushMatrix()
        run {
            gl.glMultMatrixf(transformationMatrix, 0)
            gl.glScalef(scale, scale, scale)
            gl.glEnableClientState(GL10.GL_VERTEX_ARRAY)
            gl.glLineWidth(1.0f)
            gl.glRotatef(rotation, rotationAxis[0], rotationAxis[1], rotationAxis[2])
            gl.glVertexPointer(3, GL10.GL_FLOAT, 0, verticesBuffer)
            gl.glColor4f(
                currentColor[0],
                currentColor[1],
                currentColor[2],
                currentColor[3]
            )
            for (i in 0 until triangles.size / 3) {
                trianglesBuffer.position(3 * i)
                gl.glDrawElements(GL10.GL_TRIANGLES, 3, GL10.GL_UNSIGNED_SHORT, trianglesBuffer)
            }
            trianglesBuffer.position(0)
            gl.glDisableClientState(GL10.GL_VERTEX_ARRAY)
        }
        gl.glPopMatrix()
    }

    override fun update(fracSec: Float) {
        updatePosition(fracSec)
        rotation += fracSec * angularVelocity
    }

    override fun updatePosition(fracSec: Float) {
        Matrix.translateM(
            transformationMatrix, 0, fracSec * velocity[0] * speed,
            fracSec * velocity[1] * speed,
            fracSec * velocity[2] * speed
        )
    }

    companion object {
        private var currentColor = FloatArray(4)
        private val vertices = floatArrayOf(
            0.0f, 0.0f, 1.0f,
            -0.5f, 0.0f, 0.0f,
            0.5f, 0.0f, 0.0f,
            -1.5f, 0.0f, 0.0f,
            -0.75f, 0.0f, -0.75f,
            -1.0f, 0.0f, -1.75f,
            0.0f, 0.0f, -1.25f,
            1.5f, 0.0f, 0.0f,
            0.75f, 0.0f, -0.75f,
            1.0f, 0.0f, -1.75f
        )
        private val triangles = shortArrayOf(
            0, 1, 2,
            1, 3, 4,
            4, 5, 6,
            8, 6, 9,
            2, 8, 7,
            2, 1, 4,
            4, 8, 2,
            8, 4, 6
        )
        private lateinit var verticesBuffer: FloatBuffer
        private lateinit var trianglesBuffer: ShortBuffer
        private var buffersInitialized = false
    }

    init {
        if (!buffersInitialized) {
            currentColor = floatArrayOf(1.0f, 0.875f, 0.0f, 0.7f)
            val verticesBB = ByteBuffer.allocateDirect(vertices.size * 4)
            verticesBB.order(ByteOrder.nativeOrder())
            verticesBuffer = verticesBB.asFloatBuffer()
            verticesBuffer.put(vertices)
            verticesBuffer.position(0)
            val trianglesBB = ByteBuffer.allocateDirect(triangles.size * 2)
            trianglesBB.order(ByteOrder.nativeOrder())
            trianglesBuffer = trianglesBB.asShortBuffer()
            trianglesBuffer.put(triangles)
            trianglesBuffer.position(0)
            buffersInitialized = true
        }
    }
}