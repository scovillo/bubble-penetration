package org.codeberg.scovillo.bubble.game

import android.opengl.Matrix
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import javax.microedition.khronos.opengles.GL10
import kotlin.math.sqrt

class Star(speed: Float) : GameObject(speed) {

    val score = 3

    private var rotation = 0.0f
    private val angularVelocity = 50 + Math.random().toFloat() * 100

    override fun draw(gl: GL10) {
        gl.glMatrixMode(GL10.GL_MODELVIEW)
        gl.glPushMatrix()
        gl.glMultMatrixf(transformationMatrix, 0)
        gl.glScalef(scale, scale, scale)
        gl.glRotatef(rotation, 0.0f, 1.0f, 0.0f)
        gl.glDisable(GL10.GL_CULL_FACE)
        gl.glEnableClientState(GL10.GL_VERTEX_ARRAY)

        gl.glDisable(GL10.GL_LIGHTING)
        gl.glDepthMask(false)
        gl.glBlendFunc(GL10.GL_SRC_ALPHA, GL10.GL_ONE)
        gl.glPushMatrix()
        gl.glScalef(1.10f, 1.10f, 1.10f)
        drawMesh(gl, glowMesh, 0.90f, 0.44f, 0.03f, 0.10f)
        gl.glPopMatrix()

        gl.glDepthMask(true)
        gl.glBlendFunc(GL10.GL_SRC_ALPHA, GL10.GL_ONE_MINUS_SRC_ALPHA)
        gl.glEnable(GL10.GL_LIGHTING)
        gl.glEnableClientState(GL10.GL_NORMAL_ARRAY)
        drawLitMesh(gl, sideMesh, SIDE_MATERIAL, LOW_SPECULAR, 12.0f)
        drawLitMesh(gl, bevelMesh, BEVEL_MATERIAL, GOLD_SPECULAR, 42.0f)
        drawLitMesh(gl, faceMesh, GOLD_MATERIAL, GOLD_SPECULAR, 72.0f)

        gl.glDisableClientState(GL10.GL_NORMAL_ARRAY)
        gl.glDisable(GL10.GL_LIGHTING)
        gl.glDepthMask(false)
        gl.glBlendFunc(GL10.GL_SRC_ALPHA, GL10.GL_ONE)
        drawGradientMesh(gl, faceLightMesh)
        gl.glDepthMask(true)
        gl.glBlendFunc(GL10.GL_SRC_ALPHA, GL10.GL_ONE_MINUS_SRC_ALPHA)
        gl.glDisableClientState(GL10.GL_VERTEX_ARRAY)
        gl.glEnable(GL10.GL_CULL_FACE)
        gl.glPopMatrix()
    }

    private fun drawMesh(
        gl: GL10,
        mesh: Mesh,
        red: Float,
        green: Float,
        blue: Float,
        alpha: Float,
    ) {
        mesh.vertices.position(0)
        gl.glVertexPointer(3, GL10.GL_FLOAT, 0, mesh.vertices)
        gl.glColor4f(red, green, blue, alpha)
        gl.glDrawArrays(GL10.GL_TRIANGLES, 0, mesh.vertexCount)
    }

    private fun drawLitMesh(
        gl: GL10,
        mesh: Mesh,
        material: FloatArray,
        specular: FloatArray,
        shininess: Float,
    ) {
        mesh.vertices.position(0)
        mesh.normals.position(0)
        gl.glVertexPointer(3, GL10.GL_FLOAT, 0, mesh.vertices)
        gl.glNormalPointer(GL10.GL_FLOAT, 0, mesh.normals)
        gl.glMaterialfv(GL10.GL_FRONT_AND_BACK, GL10.GL_AMBIENT_AND_DIFFUSE, material, 0)
        gl.glMaterialfv(GL10.GL_FRONT_AND_BACK, GL10.GL_SPECULAR, specular, 0)
        gl.glMaterialf(GL10.GL_FRONT_AND_BACK, GL10.GL_SHININESS, shininess)
        gl.glDrawArrays(GL10.GL_TRIANGLES, 0, mesh.vertexCount)
    }

    private fun drawGradientMesh(gl: GL10, mesh: GradientMesh) {
        mesh.vertices.position(0)
        mesh.colors.position(0)
        gl.glVertexPointer(3, GL10.GL_FLOAT, 0, mesh.vertices)
        gl.glEnableClientState(GL10.GL_COLOR_ARRAY)
        gl.glColorPointer(4, GL10.GL_FLOAT, 0, mesh.colors)
        gl.glDrawArrays(GL10.GL_TRIANGLES, 0, mesh.vertexCount)
        gl.glDisableClientState(GL10.GL_COLOR_ARRAY)
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

    private data class Mesh(
        val vertices: FloatBuffer,
        val normals: FloatBuffer,
        val vertexCount: Int,
    )

    private data class GradientMesh(
        val vertices: FloatBuffer,
        val colors: FloatBuffer,
        val vertexCount: Int,
    )

    companion object {
        private const val FACE_SCALE = 0.84f
        private const val FACE_Y = -0.20f
        private const val BEVEL_Y = -0.06f
        private const val BACK_Y = 0.16f

        private val GOLD_MATERIAL = floatArrayOf(0.82f, 0.52f, 0.035f, 1.0f)
        private val BEVEL_MATERIAL = floatArrayOf(0.68f, 0.31f, 0.015f, 1.0f)
        private val SIDE_MATERIAL = floatArrayOf(0.34f, 0.12f, 0.005f, 1.0f)
        private val GOLD_SPECULAR = floatArrayOf(0.64f, 0.54f, 0.30f, 1.0f)
        private val LOW_SPECULAR = floatArrayOf(0.24f, 0.12f, 0.015f, 1.0f)

        private val points = arrayOf(
            floatArrayOf(0.0f, 1.0f),
            floatArrayOf(-0.5f, 0.0f),
            floatArrayOf(0.5f, 0.0f),
            floatArrayOf(-1.5f, 0.0f),
            floatArrayOf(-0.75f, -0.75f),
            floatArrayOf(-1.0f, -1.75f),
            floatArrayOf(0.0f, -1.25f),
            floatArrayOf(1.5f, 0.0f),
            floatArrayOf(0.75f, -0.75f),
            floatArrayOf(1.0f, -1.75f),
        )
        private val triangles = intArrayOf(
            0, 1, 2,
            1, 3, 4,
            4, 5, 6,
            8, 6, 9,
            2, 8, 7,
            2, 1, 4,
            4, 8, 2,
            8, 4, 6,
        )
        private val perimeter = intArrayOf(0, 1, 3, 4, 5, 6, 9, 8, 7, 2)

        private val faceMesh = createFaceMesh(FACE_SCALE, FACE_Y)
        private val glowMesh = createFaceMesh(1.0f, BACK_Y + 0.01f)
        private val bevelMesh = createBevelMesh()
        private val sideMesh = createSideMesh()
        private val faceLightMesh = createFaceLightMesh()

        private fun createFaceMesh(scale: Float, y: Float): Mesh {
            val vertices = mutableListOf<Float>()
            val normals = mutableListOf<Float>()
            triangles.forEach { index ->
                vertices.add(points[index][0] * scale)
                vertices.add(y)
                vertices.add(points[index][1] * scale)
                normals.add(0.0f)
                normals.add(-1.0f)
                normals.add(0.0f)
            }
            return meshOf(vertices, normals)
        }

        private fun createFaceLightMesh(): GradientMesh {
            val vertices = mutableListOf<Float>()
            val colors = mutableListOf<Float>()
            forEachPerimeterEdge { start, end ->
                vertices.addAll(listOf(0.0f, FACE_Y - 0.01f, -0.34f))
                colors.addAll(listOf(1.0f, 0.82f, 0.30f, 0.42f))
                vertices.addAll(point(start, FACE_SCALE, FACE_Y - 0.01f).toList())
                colors.addAll(listOf(0.34f, 0.12f, 0.0f, 0.0f))
                vertices.addAll(point(end, FACE_SCALE, FACE_Y - 0.01f).toList())
                colors.addAll(listOf(0.34f, 0.12f, 0.0f, 0.0f))
            }
            return GradientMesh(bufferOf(vertices), bufferOf(colors), vertices.size / 3)
        }

        private fun createBevelMesh(): Mesh {
            val vertices = mutableListOf<Float>()
            val normals = mutableListOf<Float>()
            forEachPerimeterEdge { start, end ->
                addQuad(
                    vertices,
                    normals,
                    point(start, 1.0f, BEVEL_Y),
                    point(end, 1.0f, BEVEL_Y),
                    point(end, FACE_SCALE, FACE_Y),
                    point(start, FACE_SCALE, FACE_Y),
                    edgeNormal(start, end, -0.72f),
                )
            }
            return meshOf(vertices, normals)
        }

        private fun createSideMesh(): Mesh {
            val vertices = mutableListOf<Float>()
            val normals = mutableListOf<Float>()
            forEachPerimeterEdge { start, end ->
                addQuad(
                    vertices,
                    normals,
                    point(start, 1.0f, BACK_Y),
                    point(end, 1.0f, BACK_Y),
                    point(end, 1.0f, BEVEL_Y),
                    point(start, 1.0f, BEVEL_Y),
                    edgeNormal(start, end, 0.0f),
                )
            }
            return meshOf(vertices, normals)
        }

        private inline fun forEachPerimeterEdge(block: (Int, Int) -> Unit) {
            perimeter.indices.forEach { index ->
                block(perimeter[index], perimeter[(index + 1) % perimeter.size])
            }
        }

        private fun point(index: Int, scale: Float, y: Float) =
            floatArrayOf(points[index][0] * scale, y, points[index][1] * scale)

        private fun edgeNormal(start: Int, end: Int, normalY: Float): FloatArray {
            val deltaX = points[end][0] - points[start][0]
            val deltaZ = points[end][1] - points[start][1]
            val normalX = deltaZ
            val normalZ = -deltaX
            val length = sqrt(normalX * normalX + normalY * normalY + normalZ * normalZ)
            return floatArrayOf(normalX / length, normalY / length, normalZ / length)
        }

        private fun addQuad(
            vertices: MutableList<Float>,
            normals: MutableList<Float>,
            first: FloatArray,
            second: FloatArray,
            third: FloatArray,
            fourth: FloatArray,
            normal: FloatArray,
        ) {
            arrayOf(first, second, third, first, third, fourth).forEach { vertex ->
                vertices.addAll(vertex.toList())
                normals.addAll(normal.toList())
            }
        }

        private fun meshOf(vertices: List<Float>, normals: List<Float>): Mesh {
            return Mesh(bufferOf(vertices), bufferOf(normals), vertices.size / 3)
        }

        private fun bufferOf(values: List<Float>): FloatBuffer =
            ByteBuffer.allocateDirect(values.size * Float.SIZE_BYTES)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()
                .apply {
                    values.forEach { put(it) }
                    position(0)
                }
    }
}