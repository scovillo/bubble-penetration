package de.spicysources.bubblepenetration.screen

import android.content.Context
import android.opengl.GLSurfaceView
import android.opengl.GLU
import android.view.View
import android.widget.TextView
import de.spicysources.bubblepenetration.MainActivity
import de.spicysources.bubblepenetration.R
import de.spicysources.bubblepenetration.animation.BlinkAnimation
import de.spicysources.bubblepenetration.objects.GameObject
import de.spicysources.bubblepenetration.util.BubbleColors
import de.spicysources.bubblepenetration.util.Generator
import java.util.*
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import javax.microedition.khronos.opengles.GL11

class MenuGLSurfaceView(context: Context) : GLSurfaceView(context) {

    private val mainActivity = context as MainActivity
    private val renderer: SpaceRenderer
    private val generator: Generator
    var boundaryTop = 0f
    var boundaryBottom = 0f
    var boundaryLeft = 0f
    var boundaryRight = 0f
    private val gameObjects = ArrayList<GameObject>()
    private val objectsToBeRemoved = ArrayList<GameObject>()
    private val title: TextView
    private val titleBlinkAnimation: BlinkAnimation

    init {
        title = mainActivity.findViewById<View>(R.id.menu_title) as TextView
        titleBlinkAnimation = BlinkAnimation(title, 0.015f)
        renderer = SpaceRenderer()
        setRenderer(renderer)
        renderMode = RENDERMODE_CONTINUOUSLY
        generator = Generator()
    }

    private inner class SpaceRenderer : Renderer {

        private val modelViewScene = FloatArray(16)
        var lastFrameTime = System.currentTimeMillis()

        override fun onDrawFrame(gl: GL10) {
            // update time calculation
            val delta = System.currentTimeMillis() - lastFrameTime
            val fracSec = delta.toFloat() / 1000
            lastFrameTime = System.currentTimeMillis()
            // scene updates
            updateGameobjects(fracSec)
            // clear screen and depth buffer
            gl.glClear(GL10.GL_COLOR_BUFFER_BIT or GL10.GL_DEPTH_BUFFER_BIT)
            val gl11 = gl as GL11

            // load local system to draw scene items
            gl.glMatrixMode(GL10.GL_MODELVIEW)
            gl11.glLoadMatrixf(modelViewScene, 0)
            for (`object` in gameObjects) {
                `object`.draw(gl)
            }
        }

        private fun updateGameobjects(fracSec: Float) {
            titleBlinkAnimation.update()
            // position update on all obstacles
            for (`object` in gameObjects) {
                `object`.update(fracSec)
            }
            // check for gameobjects that flew out of the viewing area and remove
            // or deactivate them
            for (`object` in gameObjects) {
                // offset makes sure that the gameobjects don't get deleted or set
                // inactive while visible to the player.
                val offset = `object`.scale
                if (`object`.x > boundaryRight + offset
                    || `object`.x < boundaryLeft - offset
                    || `object`.z > boundaryTop + offset
                    || `object`.z < boundaryBottom - offset
                ) {
                    objectsToBeRemoved.add(`object`)
                }
            }
            // remove obsolete gameobjects
            for (`object` in objectsToBeRemoved) {
                gameObjects.remove(`object`)
            }
            objectsToBeRemoved.clear()
            //add new gameobjects
            generator.generateGameobject(
                gameObjects,
                BubbleColors.RED,
                boundaryBottom,
                boundaryTop,
                boundaryRight,
                boundaryLeft
            )
        }

        // Called when surface is created or the viewport gets resized
        // set projection matrix
        // precalculate modelview matrix
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
            val desired_height = 10.0f
            // We want to be able to see the range of 5 to -5 units at the y
            // axis (height=10).
            // To achieve this we have to pull the camera towards the positive z axis
            // based on the following formula:
            // z = (desired_height / 2) / tan(fovy/2)
            val z =
                (desired_height / 2 / Math.tan(fovy / 2 * (Math.PI / 180.0f))).toFloat()
            // forward for the camera is backward for the scene
            gl.glTranslatef(0.0f, 0.0f, -z)
            // rotate local to achive top down view from negative y down to xz-plane
            // z range is the desired height
            gl.glRotatef(-90.0f, 1.0f, 0.0f, 0.0f)
            // save local system as a basis to draw scene items
            gl11.glGetFloatv(GL11.GL_MODELVIEW_MATRIX, modelViewScene, 0)
            // window boundaries
            // z range is the desired height
            boundaryTop = desired_height / 2
            boundaryBottom = -desired_height / 2
            // x range is the desired width
            boundaryLeft = -(desired_height / 2 * aspectRatio)
            boundaryRight = desired_height / 2 * aspectRatio
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