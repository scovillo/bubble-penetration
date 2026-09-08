package org.codeberg.scovillo.bubble.ui.render

import android.opengl.GLSurfaceView
import org.codeberg.scovillo.bubble.game.Boundaries
import org.codeberg.scovillo.bubble.game.MatchState
import org.codeberg.scovillo.bubble.game.generator.MatchEngine
import org.codeberg.scovillo.bubble.game.generator.MatchEngineConfig
import org.codeberg.scovillo.bubble.game.generator.RandomizedMatchEngine
import java.lang.Math.random
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.math.roundToInt

class MenuBubbleScene(
    private val boundaries: Boundaries = Boundaries(),
    state: MatchState = MatchState(score = 50 + (750 * random()).roundToInt()),
    private val generator: MatchEngine =
        RandomizedMatchEngine(
            state = state,
            config = MatchEngineConfig("", -1),
            boundaries = boundaries
        )
) : BubbleScene {

    override val renderer: GLSurfaceView.Renderer = MenuRenderer()
    private val openGlScene = OpenGlScene(boundaries, SceneLighting.MENU)

    private inner class MenuRenderer : GLSurfaceView.Renderer {

        override fun onDrawFrame(gl: GL10) {
            val fracSec = openGlScene.elapsedSeconds()
            generator.advanceVisuals(fracSec)
            openGlScene.draw(gl, generator.gameObjects)
        }

        override fun onSurfaceChanged(gl: GL10, width: Int, height: Int) {
            openGlScene.resize(gl, width, height)
        }

        override fun onSurfaceCreated(gl: GL10, config: EGLConfig) {
            openGlScene.initialize(gl)
        }
    }
}
