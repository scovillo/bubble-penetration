package org.codeberg.scovillo.bubble.game

import android.opengl.Matrix
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.ShortBuffer
import javax.microedition.khronos.opengles.GL10
import kotlin.math.sqrt

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
            // Keep the light effect gentle on the flat star mesh.
            gl.glDepthMask(false)
            gl.glBlendFunc(GL10.GL_SRC_ALPHA, GL10.GL_ONE_MINUS_SRC_ALPHA)
            gl.glEnable(GL10.GL_LIGHTING)
            gl.glMaterialfv(GL10.GL_FRONT_AND_BACK, GL10.GL_AMBIENT_AND_DIFFUSE, GOLD_MATERIAL, 0)
            gl.glMaterialfv(GL10.GL_FRONT_AND_BACK, GL10.GL_SPECULAR, GOLD_SPECULAR, 0)
            gl.glMaterialfv(GL10.GL_FRONT_AND_BACK, GL10.GL_EMISSION, GOLD_EMISSION, 0)
            gl.glMaterialf(GL10.GL_FRONT_AND_BACK, GL10.GL_SHININESS, 38.0f)
            gl.glEnableClientState(GL10.GL_NORMAL_ARRAY)
            normalsBuffer.position(0)
            gl.glNormalPointer(GL10.GL_FLOAT, 0, normalsBuffer)
            drawLitTriangles(gl)
            gl.glMaterialfv(GL10.GL_FRONT_AND_BACK, GL10.GL_EMISSION, NO_EMISSION, 0)
            gl.glDisableClientState(GL10.GL_NORMAL_ARRAY)
            gl.glDisable(GL10.GL_LIGHTING)
            gl.glBlendFunc(GL10.GL_SRC_ALPHA, GL10.GL_ONE)
            drawTriangles(gl, 0.86f, 1.0f, 0.90f, 0.42f, 0.13f)
            gl.glBlendFunc(GL10.GL_SRC_ALPHA, GL10.GL_ONE_MINUS_SRC_ALPHA)
            gl.glDepthMask(true)
            trianglesBuffer.position(0)
            gl.glDisableClientState(GL10.GL_VERTEX_ARRAY)
        }
        gl.glPopMatrix()
    }

    private fun drawTriangles(gl: GL10, scale: Float, red: Float, green: Float, blue: Float, alpha: Float) {
        gl.glPushMatrix()
        gl.glScalef(scale, scale, scale)
        gl.glColor4f(red, green, blue, alpha)
        for (i in 0 until triangles.size / 3) {
            trianglesBuffer.position(3 * i)
            gl.glDrawElements(GL10.GL_TRIANGLES, 3, GL10.GL_UNSIGNED_SHORT, trianglesBuffer)
        }
        gl.glPopMatrix()
    }

    private fun drawLitTriangles(gl: GL10) {
        for (i in 0 until triangles.size / 3) {
            trianglesBuffer.position(3 * i)
            gl.glDrawElements(GL10.GL_TRIANGLES, 3, GL10.GL_UNSIGNED_SHORT, trianglesBuffer)
        }
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
        private val GOLD_MATERIAL = floatArrayOf(1.0f, 0.82f, 0.10f, 1.0f)
        private val GOLD_SPECULAR = floatArrayOf(1.0f, 0.90f, 0.62f, 1.0f)
        private val GOLD_EMISSION = floatArrayOf(0.30f, 0.19f, 0.02f, 1.0f)
        private val NO_EMISSION = floatArrayOf(0.0f, 0.0f, 0.0f, 1.0f)
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
        private lateinit var normalsBuffer: FloatBuffer
        private lateinit var trianglesBuffer: ShortBuffer
        private var buffersInitialized = false
    }

    init {
        if (!buffersInitialized) {
            val verticesBB = ByteBuffer.allocateDirect(vertices.size * 4)
            verticesBB.order(ByteOrder.nativeOrder())
            verticesBuffer = verticesBB.asFloatBuffer()
            verticesBuffer.put(vertices)
            verticesBuffer.position(0)
            val normalsBB = ByteBuffer.allocateDirect(vertices.size * 4)
            normalsBB.order(ByteOrder.nativeOrder())
            normalsBuffer = normalsBB.asFloatBuffer()
            // Tilt the normals away from the centre to simulate a gently domed coin.
            for (index in vertices.indices step 3) {
                val normalX = -vertices[index] * 0.42f
                val normalY = 0.90f
                val normalZ = -(vertices[index + 2] + 0.45f) * 0.42f
                val length = sqrt(normalX * normalX + normalY * normalY + normalZ * normalZ)
                normalsBuffer.put(normalX / length).put(normalY / length).put(normalZ / length)
            }
            normalsBuffer.position(0)
            val trianglesBB = ByteBuffer.allocateDirect(triangles.size * 2)
            trianglesBB.order(ByteOrder.nativeOrder())
            trianglesBuffer = trianglesBB.asShortBuffer()
            trianglesBuffer.put(triangles)
            trianglesBuffer.position(0)
            buffersInitialized = true
        }
    }
}
