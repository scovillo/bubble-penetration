package org.codeberg.scovillo.bubble.android.game

import android.opengl.Matrix
import org.codeberg.scovillo.bubble.game.GameObject
import javax.microedition.khronos.opengles.GL10

/** Android/OpenGL projection of a platform-neutral [org.codeberg.scovillo.bubble.game.GameObject]. */
abstract class OpenGlObject(state: GameObject) {

    val transformationMatrix: FloatArray = FloatArray(16)

    init {
        Matrix.setIdentityM(transformationMatrix, 0)
        Matrix.translateM(transformationMatrix, 0, state.x, state.y, state.z)
    }

    abstract fun draw(gl: GL10)
}
