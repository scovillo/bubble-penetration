package de.spicysources.bubblepenetration.objects;

import android.opengl.Matrix;

import javax.microedition.khronos.opengles.GL10;

public abstract class GameObject {
    // current transformation matrix
    public float[] transformationMatrix;
    // current velocity (x,y,z)
    public float[] velocity;
    // current y-rotation, positive is z to x direction; angle zero is z-axis
    public float yRot;

    public float scale = 1.0f;

    public static float speed = 1.0f;	// alternative way to control speed, additional scaling factor

    public GameObject() {
        transformationMatrix = new float[16];
        velocity = new float[3];
        Matrix.setIdentityM(transformationMatrix, 0);
    }

    public abstract void draw(GL10 gl);

    public abstract void update(float fracSec);

    public void setVelocity(float vx, float vy, float vz) {
        velocity[0] = vx;
        velocity[1] = vy;
        velocity[2] = vz;
    }

    public void setYRot()
    {
        if(velocity[0]*velocity[0]+velocity[1]*velocity[1]+velocity[2]*velocity[2] > 1E-20)
            yRot=(float)(Math.acos(velocity[2]/Math.sqrt(velocity[0]*velocity[0]+velocity[2]*velocity[2]))*180/Math.PI);
        if(velocity[0]<0)
            yRot=-yRot;
    }

    protected void updatePosition(float fracSec) {
        Matrix.translateM(transformationMatrix, 0, fracSec*velocity[0] * speed,
                fracSec*velocity[1] * speed,
                fracSec*velocity[2] * speed);
    }

    public void setPosition(float x, float y, float z) {
        Matrix.setIdentityM(transformationMatrix, 0);
        Matrix.translateM(transformationMatrix, 0, x, y, z);
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

    public float getX() {
        return transformationMatrix[12];
    }

    public float getY() {
        return transformationMatrix[13];
    }

    public float getZ() {
        return transformationMatrix[14];
    }

    public void setX(float x) {
        transformationMatrix[12] = x;
    }

    public void setY(float y) {
        transformationMatrix[13] = y;
    }

    public void setZ(float z) {
        transformationMatrix[14] = z;
    }
}

