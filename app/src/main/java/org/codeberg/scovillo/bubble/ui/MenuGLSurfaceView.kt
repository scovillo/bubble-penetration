package org.codeberg.scovillo.bubble.ui

import android.content.Context
import android.opengl.GLSurfaceView
import android.opengl.GLU
import org.codeberg.scovillo.bubble.game.BubbleColors
import org.codeberg.scovillo.bubble.game.GameObject
import org.codeberg.scovillo.bubble.game.Generator
import java.lang.Math.PI
import java.lang.Math.random
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import javax.microedition.khronos.opengles.GL11
import kotlin.math.roundToInt
import kotlin.math.tan


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
            val fovy = 45.0f
            // set up projection matrix for scene
            gl.glMatrixMode(GL10.GL_PROJECTION)
            gl.glLoadIdentity()
            GLU.gluPerspective(gl, fovy, aspectRatio, 0.001f, 100.0f)
            // set up modelview matrix for scene
            gl.glMatrixMode(GL10.GL_MODELVIEW)
            gl.glLoadIdentity()
            val desiredHeight = if (aspectRatio > 1.0f) 10.0f else 10.0f / aspectRatio
            // We want to be able to see the range of 5 to -5 units at the y
            // axis (height=10).
            // To achieve this we have to pull the camera towards the positive z axis
            // based on the following formula:
            // z = (desired_height / 2) / tan(fovy/2)
            val z = (desiredHeight / 2 / tan(fovy / 2 * (PI / 180.0f))).toFloat()
            // forward for the camera is backward for the scene
            gl.glTranslatef(0.0f, 0.0f, -z)
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
            gl.glClearColor(0.0f, 0.0f, 0.0f, 0.0f)
            gl.glEnable(GL10.GL_CULL_FACE)
            gl.glShadeModel(GL10.GL_FLAT)
            gl.glEnable(GL10.GL_DEPTH_TEST)
            gl.glDepthFunc(GL10.GL_LEQUAL)
            gl.glShadeModel(GL10.GL_SMOOTH)
            gl.glEnable(GL10.GL_DEPTH_TEST)
        }

    }

}