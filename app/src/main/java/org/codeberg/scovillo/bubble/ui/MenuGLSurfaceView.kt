package org.codeberg.scovillo.bubble.ui

import android.content.Context
import android.opengl.GLSurfaceView
import org.codeberg.scovillo.bubble.game.BubbleColors
import org.codeberg.scovillo.bubble.game.GameObject
import org.codeberg.scovillo.bubble.game.Generator
import java.lang.Math.random
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import javax.microedition.khronos.opengles.GL11
import kotlin.math.roundToInt


class MenuGLSurfaceView(context: Context) : GLSurfaceView(context) {

    private val renderer: SpaceRenderer
    private val boundaries = Boundaries()
    private val gameObjects = ArrayList<GameObject>()
    private val generator = Generator(gameObjects, boundaries)
    private val objectsToBeRemoved = ArrayList<GameObject>()
    private val score = (800 * random()).roundToInt()

    init {
        renderer = SpaceRenderer()
        setRenderer(renderer)
        renderMode = RENDERMODE_CONTINUOUSLY
    }

    private inner class SpaceRenderer : Renderer {

        private val modelViewScene = FloatArray(16)
        var lastFrameTime = System.currentTimeMillis()

        override fun onDrawFrame(gl: GL10) {
            val delta = System.currentTimeMillis() - lastFrameTime
            val fracSec = delta.toFloat() / 1000
            lastFrameTime = System.currentTimeMillis()
            updateGameobjects(fracSec)
            gl.glClear(GL10.GL_COLOR_BUFFER_BIT or GL10.GL_DEPTH_BUFFER_BIT)
            val gl11 = gl as GL11
            gl.glMatrixMode(GL10.GL_MODELVIEW)
            gl11.glLoadMatrixf(modelViewScene, 0)
            gameObjects.forEach { it.draw(gl) }
        }

        private fun updateGameobjects(fracSec: Float) {
            gameObjects.forEach { it.update(fracSec) }
            gameObjects.forEach {
                val offset = it.scale
                if (it.x > boundaries.right + offset
                    || it.x < boundaries.left - offset
                    || it.z > boundaries.top + offset
                    || it.z < boundaries.bottom - offset
                ) {
                    objectsToBeRemoved.add(it)
                }
            }
            objectsToBeRemoved.forEach { gameObjects.remove(it) }
            objectsToBeRemoved.clear()
            generator.generateGameobject(BubbleColors.RED, score)
        }

        override fun onSurfaceChanged(gl: GL10, width: Int, height: Int) {
            val gl11 = gl as GL11
            gl.glViewport(0, 0, width, height)
            val aspectRatio = width.toFloat() / height
            gl.glMatrixMode(GL10.GL_PROJECTION)
            gl.glLoadIdentity()
            val desiredHeight = if (aspectRatio > 1.0f) 10.0f else 10.0f / aspectRatio
            val desiredWidth = desiredHeight * aspectRatio
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
            // rotate local to achieve top down view from negative y down to xz-plane
            // z range is the desired height
            gl.glRotatef(-90.0f, 1.0f, 0.0f, 0.0f)
            // save local system as a basis to draw scene items
            gl11.glGetFloatv(GL11.GL_MODELVIEW_MATRIX, modelViewScene, 0)
            boundaries.updateWith(desiredHeight, aspectRatio)
        }

        override fun onSurfaceCreated(gl: GL10, config: EGLConfig) {
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
            gl.glEnable(GL10.GL_DEPTH_TEST)
            // Bubble.draw() uses lighting for its glossy finish; the menu needs the same light.
            gl.glEnable(GL10.GL_NORMALIZE)
            gl.glLightfv(GL10.GL_LIGHT0, GL10.GL_AMBIENT, floatArrayOf(0.32f, 0.32f, 0.42f, 1.0f), 0)
            gl.glLightfv(GL10.GL_LIGHT0, GL10.GL_DIFFUSE, floatArrayOf(0.78f, 0.82f, 0.92f, 1.0f), 0)
            gl.glLightfv(GL10.GL_LIGHT0, GL10.GL_SPECULAR, floatArrayOf(0.76f, 0.80f, 0.92f, 1.0f), 0)
            gl.glLightfv(GL10.GL_LIGHT0, GL10.GL_POSITION, floatArrayOf(-4.0f, 8.0f, 6.0f, 1.0f), 0)
            gl.glEnable(GL10.GL_LIGHT0)
        }

    }

}
