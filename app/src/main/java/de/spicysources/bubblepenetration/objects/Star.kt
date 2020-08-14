package de.spicysources.bubblepenetration.objects;

import android.opengl.Matrix;

import javax.microedition.khronos.opengles.GL10;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

public class Star extends GameObject {
    private static float currentColor[] = new float[4];
    // current rotation
    public float rotation = 0.0f;
    // rotation speed in deg/s
    public float angularVelocity =  50 + (float)Math.random()*100;
    public float rotationAxis[] = {0.0f, 1.0f, 0.0f};
    // doubled bubble speed
    public float speed = GameObject.speed * 2;

    private static final float vertices[] = new float[]{
            0.0f, 0.0f, 1.0f,   //0
            -0.5f, 0.0f, 0.0f,  //1
            0.5f, 0.0f, 0.0f,   //2
            -1.5f, 0.0f, 0.0f,  //3
            -0.75f, 0.0f, -0.75f, //4
            -1.0f, 0.0f, -1.75f, //5
            0.0f, 0.0f, -1.25f,  //6
            1.5f, 0.0f, 0.0f,   //7
            0.75f, 0.0f, -0.75f,  //8
            1.0f, 0.0f, -1.75f,  //9
    };

    private static final short triangles[] = new short[]{
            0,1,2,
            1,3,4,
            4,5,6,
            8,6,9,
            2,8,7,
            2,1,4,
            4,8,2,
            8,4,6
    };

    private static FloatBuffer verticesBuffer;
    private static ShortBuffer trianglesBuffer;
    private static boolean buffersInitialized = false;

    public Star() {
        if(!buffersInitialized) {
            currentColor = new float[]{1.0f, 1.0f, 1.0f, 0.7f};

            // Initialize buffers
            ByteBuffer verticesBB = ByteBuffer.allocateDirect(vertices.length * 4);
            verticesBB.order(ByteOrder.nativeOrder());
            verticesBuffer = verticesBB.asFloatBuffer();
            verticesBuffer.put(vertices);
            verticesBuffer.position(0);

            ByteBuffer trianglesBB = ByteBuffer.allocateDirect(triangles.length * 2);
            trianglesBB.order(ByteOrder.nativeOrder());
            trianglesBuffer = trianglesBB.asShortBuffer();
            trianglesBuffer.put(triangles);
            trianglesBuffer.position(0);

            buffersInitialized = true;
        }
    }

    @Override
    public void draw(GL10 gl) {
        gl.glMatrixMode(GL10.GL_MODELVIEW);
        gl.glPushMatrix();
        {
            gl.glMultMatrixf(transformationMatrix, 0);
            gl.glScalef(scale, scale, scale);

            gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);

            gl.glLineWidth(1.0f);

            gl.glRotatef(rotation, rotationAxis[0], rotationAxis[1], rotationAxis[2]);
            gl.glVertexPointer(3, GL10.GL_FLOAT, 0, verticesBuffer);
            gl.glColor4f(currentColor[0], currentColor[1], currentColor[2], currentColor[3]);
            for (int i = 0; i < (triangles.length / 3); i++) {
                trianglesBuffer.position(3 * i);
                gl.glDrawElements(GL10.GL_TRIANGLES, 3, GL10.GL_UNSIGNED_SHORT, trianglesBuffer);
            }
            trianglesBuffer.position(0);

            gl.glDisableClientState(GL10.GL_VERTEX_ARRAY);
        }
        gl.glPopMatrix();
    }

    @Override
    public void update(float fracSec) {
        updatePosition(fracSec);
        rotation += fracSec * angularVelocity;
    }

    @Override
    protected void updatePosition(float fracSec) {
        Matrix.translateM(transformationMatrix, 0, fracSec*velocity[0] * speed,
                fracSec*velocity[1] * speed,
                fracSec*velocity[2] * speed);
    }
}
