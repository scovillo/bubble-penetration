package org.codeberg.scovillo.bubble.game

import android.opengl.Matrix
import javax.microedition.khronos.opengles.GL10

/** Android/OpenGL projection of a platform-neutral [GameObject]. */
abstract class UiGameObject(val state: GameObject) {

    var transformationMatrix: FloatArray = FloatArray(16)
    abstract fun draw(gl: GL10)

    init {
        Matrix.setIdentityM(transformationMatrix, 0)
        Matrix.translateM(transformationMatrix, 0, state.x, state.y, state.z)
    }
}
