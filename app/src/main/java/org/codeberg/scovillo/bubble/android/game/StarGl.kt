package org.codeberg.scovillo.bubble.android.game

import org.codeberg.scovillo.bubble.game.GameObject
import org.codeberg.scovillo.bubble.game.MeshData
import org.codeberg.scovillo.bubble.game.StarMeshFactory
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import javax.microedition.khronos.opengles.GL10

class StarGl(private val state: GameObject) : OpenGlObject(state) {

    val score get() = state.score

    override fun draw(gl: GL10) {
        val popProgress = (state.disappearElapsedSeconds / POP_DURATION_SECONDS).coerceIn(0f, 1f)
        val rotation = state.ageSeconds * 90f
        val popScale = if (state.isDisappearing) 1f - popProgress else 1f
        gl.glMatrixMode(GL10.GL_MODELVIEW)
        gl.glPushMatrix()
        gl.glMultMatrixf(transformationMatrix, 0)
        gl.glScalef(state.scale * popScale, state.scale * popScale, state.scale * popScale)
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

    private data class Mesh(
        val vertices: FloatBuffer,
        val normals: FloatBuffer,
        val vertexCount: Int
    )

    private data class GradientMesh(
        val vertices: FloatBuffer,
        val colors: FloatBuffer,
        val vertexCount: Int
    )

    companion object {
        private const val POP_DURATION_SECONDS = 0.22f
        private val GOLD_MATERIAL = floatArrayOf(0.82f, 0.52f, 0.035f, 1.0f)
        private val BEVEL_MATERIAL = floatArrayOf(0.68f, 0.31f, 0.015f, 1.0f)
        private val SIDE_MATERIAL = floatArrayOf(0.34f, 0.12f, 0.005f, 1.0f)
        private val GOLD_SPECULAR = floatArrayOf(0.64f, 0.54f, 0.30f, 1.0f)
        private val LOW_SPECULAR = floatArrayOf(0.24f, 0.12f, 0.015f, 1.0f)

        private val coreMeshes = StarMeshFactory.create()
        private val faceMesh = meshOf(coreMeshes.face)
        private val glowMesh = meshOf(coreMeshes.glow)
        private val bevelMesh = meshOf(coreMeshes.bevel)
        private val sideMesh = meshOf(coreMeshes.side)
        private val faceLightMesh = gradientMeshOf(coreMeshes.faceLight)

        private fun meshOf(data: MeshData) = Mesh(
            bufferOf(data.vertices), bufferOf(requireNotNull(data.normals)), data.vertexCount
        )

        private fun gradientMeshOf(data: MeshData) = GradientMesh(
            bufferOf(data.vertices), bufferOf(requireNotNull(data.colors)), data.vertexCount
        )

        private fun bufferOf(values: FloatArray): FloatBuffer =
            ByteBuffer.allocateDirect(values.size * Float.SIZE_BYTES)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()
                .apply { put(values); position(0) }
    }
}
