package org.codeberg.scovillo.bubble.ui.render

import android.content.Context
import android.opengl.GLSurfaceView
import android.view.MotionEvent

interface BubbleScene {
    val renderer: GLSurfaceView.Renderer

    fun onTouchEvent(event: MotionEvent): Boolean = false

    fun onResume() = Unit
}

private class EmptyBubbleScene : BubbleScene {
    override val renderer = object : GLSurfaceView.Renderer {
        override fun onDrawFrame(gl: javax.microedition.khronos.opengles.GL10) = Unit

        override fun onSurfaceChanged(
            gl: javax.microedition.khronos.opengles.GL10,
            width: Int,
            height: Int,
        ) = Unit

        override fun onSurfaceCreated(
            gl: javax.microedition.khronos.opengles.GL10,
            config: javax.microedition.khronos.egl.EGLConfig,
        ) = Unit
    }
}

class BubbleGLSurfaceView(
    context: Context,
    private val scene: BubbleScene,
) : GLSurfaceView(context) {

    private constructor(context: Context) : this(context, EmptyBubbleScene())

    init {
        setRenderer(scene.renderer)
        renderMode = RENDERMODE_CONTINUOUSLY
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (scene.onTouchEvent(event)) {
            performClick()
        }
        return true
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }

    fun pauseScene() {
        onPause()
    }

    fun resumeScene() {
        scene.onResume()
        onResume()
    }
}
