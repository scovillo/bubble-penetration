package org.codeberg.scovillo.bubble.ui.render

import android.opengl.GLSurfaceView
import org.codeberg.scovillo.bubble.game.BubbleColors
import org.codeberg.scovillo.bubble.game.GameObject
import org.codeberg.scovillo.bubble.game.Generator
import java.lang.Math.random
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.math.roundToInt

class MenuBubbleScene : BubbleScene {

    override val renderer: GLSurfaceView.Renderer = MenuRenderer()
    private val boundaries = Boundaries()
    private val openGlScene = OpenGlScene(boundaries, SceneLighting.MENU)
    private val gameObjects = ArrayList<GameObject>()
    private val generator = Generator(gameObjects, boundaries)
    private val objectsToBeRemoved = ArrayList<GameObject>()
    private val score = (800 * random()).roundToInt()

    private inner class MenuRenderer : GLSurfaceView.Renderer {

        override fun onDrawFrame(gl: GL10) {
            val fracSec = openGlScene.elapsedSeconds()
            updateGameobjects(fracSec)
            openGlScene.draw(gl, gameObjects)
        }

        private fun updateGameobjects(fracSec: Float) {
            gameObjects.forEach { it.update(fracSec) }
            gameObjects.forEach {
                if (it.isOutside(boundaries)) {
                    objectsToBeRemoved.add(it)
                }
            }
            objectsToBeRemoved.forEach { gameObjects.remove(it) }
            objectsToBeRemoved.clear()
            generator.generateGameobject(BubbleColors.RED, score)
        }

        override fun onSurfaceChanged(gl: GL10, width: Int, height: Int) {
            openGlScene.resize(gl, width, height)
        }

        override fun onSurfaceCreated(gl: GL10, config: EGLConfig) {
            openGlScene.initialize(gl)
        }
    }
}
