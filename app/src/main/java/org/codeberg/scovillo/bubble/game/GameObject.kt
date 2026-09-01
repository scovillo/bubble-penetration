package org.codeberg.scovillo.bubble.game

import android.opengl.Matrix
import javax.microedition.khronos.opengles.GL10

abstract class GameObject(val speed: Float) {
    var transformationMatrix: FloatArray = FloatArray(16)
    var velocity: FloatArray = FloatArray(3)
    var scale = 1.0f

    abstract fun draw(gl: GL10)
    abstract fun update(fracSec: Float)

    protected open fun updatePosition(fracSec: Float) {
        Matrix.translateM(
            transformationMatrix, 0, fracSec * velocity[0] * speed,
            fracSec * velocity[1] * speed,
            fracSec * velocity[2] * speed
        )
    }

    fun setPosition(x: Float, y: Float, z: Float) {
        Matrix.setIdentityM(transformationMatrix, 0)
        Matrix.translateM(transformationMatrix, 0, x, y, z)
    }

    /*
	 * An OpenGL transformation matrix has the following format:
	 * Values:    Indices:
	 *   v v v x    0  4  8 12
	 *   v v v y    1  5  9 13
	 *   v v v z    2  6 10 14
	 *   v v v v    3  7 11 15 * While the values marked with v are based on all the transformation that
	 * were done at the matrix, the values marked with x, y and z contain the
	 * coordinates. With that in mind we can provide the following convenience
	 * functions to provide easy access to those values */
    var x: Float
        get() = transformationMatrix[12]
        set(x) {
            transformationMatrix[12] = x
        }

    var y: Float
        get() = transformationMatrix[13]
        set(y) {
            transformationMatrix[13] = y
        }

    var z: Float
        get() = transformationMatrix[14]
        set(z) {
            transformationMatrix[14] = z
        }

    init {
        Matrix.setIdentityM(transformationMatrix, 0)
    }
}