package org.codeberg.scovillo.bubble.ui.render

import org.codeberg.scovillo.bubble.game.GameObject
import javax.microedition.khronos.opengles.GL10
import javax.microedition.khronos.opengles.GL11

class OpenGlScene(
    private val boundaries: Boundaries,
    private val lighting: SceneLighting,
) {

    private val modelViewScene = FloatArray(16)
    private var lastFrameTime = System.currentTimeMillis()

    fun resetFrameTime() {
        lastFrameTime = System.currentTimeMillis()
    }

    fun elapsedSeconds(): Float {
        val currentFrameTime = System.currentTimeMillis()
        val elapsedSeconds = (currentFrameTime - lastFrameTime).toFloat() / 1000
        lastFrameTime = currentFrameTime
        return elapsedSeconds
    }

    fun draw(gl: GL10, gameObjects: Iterable<GameObject>) {
        gl.glClear(GL10.GL_COLOR_BUFFER_BIT or GL10.GL_DEPTH_BUFFER_BIT)
        gl.glMatrixMode(GL10.GL_MODELVIEW)
        (gl as GL11).glLoadMatrixf(modelViewScene, 0)
        gameObjects.forEach { it.draw(gl) }
    }

    fun resize(gl: GL10, width: Int, height: Int): SceneDimensions {
        val aspectRatio = width.toFloat() / height
        val desiredHeight = if (aspectRatio > 1.0f) 10.0f else 10.0f / aspectRatio
        val desiredWidth = desiredHeight * aspectRatio

        gl.glViewport(0, 0, width, height)
        gl.glMatrixMode(GL10.GL_PROJECTION)
        gl.glLoadIdentity()
        gl.glOrthof(
            -desiredWidth / 2,
            desiredWidth / 2,
            -desiredHeight / 2,
            desiredHeight / 2,
            0.001f,
            100.0f,
        )
        gl.glMatrixMode(GL10.GL_MODELVIEW)
        gl.glLoadIdentity()
        gl.glTranslatef(0.0f, 0.0f, -20.0f)
        gl.glRotatef(-90.0f, 1.0f, 0.0f, 0.0f)
        (gl as GL11).glGetFloatv(GL11.GL_MODELVIEW_MATRIX, modelViewScene, 0)
        boundaries.updateWith(desiredHeight, aspectRatio)

        return SceneDimensions(desiredHeight, aspectRatio)
    }

    fun initialize(gl: GL10) {
        gl.glDisable(GL10.GL_DITHER)
        gl.glHint(GL10.GL_PERSPECTIVE_CORRECTION_HINT, GL10.GL_FASTEST)
        gl.glBlendFunc(GL10.GL_SRC_ALPHA, GL10.GL_ONE_MINUS_SRC_ALPHA)
        gl.glEnable(GL10.GL_BLEND)
        gl.glClearColor(0.027f, 0.035f, 0.11f, 1.0f)
        gl.glEnable(GL10.GL_CULL_FACE)
        gl.glShadeModel(GL10.GL_FLAT)
        gl.glEnable(GL10.GL_DEPTH_TEST)
        gl.glDepthFunc(GL10.GL_LEQUAL)
        gl.glShadeModel(GL10.GL_SMOOTH)
        gl.glEnable(GL10.GL_NORMALIZE)
        lighting.applyTo(gl)
    }
}

data class SceneDimensions(
    val height: Float,
    val aspectRatio: Float,
)

enum class SceneLighting {
    GAME,
    MENU;

    fun applyTo(gl: GL10) {
        val ambient: FloatArray
        val diffuse: FloatArray
        val specular = floatArrayOf(0.76f, 0.80f, 0.92f, 1.0f)
        val position = floatArrayOf(-4.0f, 8.0f, 6.0f, 1.0f)

        when (this) {
            GAME -> {
                ambient = floatArrayOf(0.20f, 0.20f, 0.30f, 1.0f)
                diffuse = floatArrayOf(0.72f, 0.76f, 0.88f, 1.0f)
            }
            MENU -> {
                ambient = floatArrayOf(0.32f, 0.32f, 0.42f, 1.0f)
                diffuse = floatArrayOf(0.78f, 0.82f, 0.92f, 1.0f)
            }
        }

        gl.glLightfv(GL10.GL_LIGHT0, GL10.GL_AMBIENT, ambient, 0)
        gl.glLightfv(GL10.GL_LIGHT0, GL10.GL_DIFFUSE, diffuse, 0)
        gl.glLightfv(GL10.GL_LIGHT0, GL10.GL_SPECULAR, specular, 0)
        gl.glLightfv(GL10.GL_LIGHT0, GL10.GL_POSITION, position, 0)
        gl.glEnable(GL10.GL_LIGHT0)
    }
}

fun GameObject.isOutside(boundaries: Boundaries): Boolean {
    val offset = scale
    return x > boundaries.right + offset ||
        x < boundaries.left - offset ||
        z > boundaries.top + offset ||
        z < boundaries.bottom - offset
}
