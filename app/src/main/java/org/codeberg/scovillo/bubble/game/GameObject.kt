package org.codeberg.scovillo.bubble.game

import android.opengl.Matrix
import javax.microedition.khronos.opengles.GL10

abstract class GameObject(val speed: Float) {
    // current transformation matrix
    var transformationMatrix: FloatArray

    // current velocity (x,y,z)
    var velocity: FloatArray

    // current y-rotation, positive is z to x direction; angle zero is z-axis
    var yRot = 0f
    var scale = 1.0f
    abstract fun draw(gl: GL10)
    abstract fun update(fracSec: Float)
    fun setVelocity(vx: Float, vy: Float, vz: Float) {
        velocity[0] = vx
        velocity[1] = vy
        velocity[2] = vz
    }

    fun setYRot() {
        if (velocity[0] * velocity[0] + velocity[1] * velocity[1] + velocity[2] * velocity[2] > 1E-20
        ) yRot = (Math.acos(
            velocity[2] / Math.sqrt(
                velocity[0] * velocity[0] + velocity[2] * velocity[2].toDouble()
            )
        ) * 180 / Math.PI).toFloat()
        if (velocity[0] < 0) yRot = -yRot
    }

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
        transformationMatrix = FloatArray(16)
        velocity = FloatArray(3)
        Matrix.setIdentityM(transformationMatrix, 0)
    }
}