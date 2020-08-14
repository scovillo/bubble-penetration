package de.spicysources.bubblepenetration.objects;

import de.spicysources.bubblepenetration.util.BubbleColors;

import javax.microedition.khronos.opengles.GL10;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

public class Bubble extends GameObject {
    //Wobble animation scale factors
    private float wobbleX = 1.0f;
    private float wobbleY = 0.7f;

    //Wobble animation flags
    private boolean wobbleXup = true;
    private boolean wobbleYup = true;

    //Object color
    private BubbleColors color;
    //Matching GLColor
    private float glColor[] = new float[4];

    public Bubble(BubbleColors color, float[] glColor) {
        this.color = color;
        this.glColor = glColor;
    }

    @Override
    public void draw(GL10 gl) {
        gl.glMatrixMode(GL10.GL_MODELVIEW);
        gl.glPushMatrix();
        {
            gl.glMultMatrixf(transformationMatrix, 0);
            gl.glScalef(scale*wobbleX, scale, scale*wobbleY);
            gl.glColor4f(glColor[0], glColor[1], glColor[2], glColor[3]);

            float	angleA, angleB;
            float	cos, sin;
            float	r1, r2;
            float	h1, h2;
            float	step = 30.0f;
            float[][] v = new float[32][3];
            ByteBuffer vbb;
            FloatBuffer vBuf;

            vbb = ByteBuffer.allocateDirect(v.length * v[0].length * 4);
            vbb.order(ByteOrder.nativeOrder());
            vBuf = vbb.asFloatBuffer();

            gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
            gl.glEnableClientState(GL10.GL_NORMAL_ARRAY);

            for (angleA = -90.0f; angleA <90.0f; angleA += step) {
                int	n = 0;

                r1 = (float)Math.cos(angleA * Math.PI / 180.0);
                r2 = (float)Math.cos((angleA + step) * Math.PI / 180.0);
                h1 = (float)Math.sin(angleA * Math.PI / 180.0);
                h2 = (float)Math.sin((angleA + step) * Math.PI / 180.0);

                // Fixed latitude, 360 degrees rotation to traverse a weft
                for (angleB = 0.0f; angleB <= 360.0f; angleB += step) {

                    cos = (float)Math.cos(angleB * Math.PI / 180.0);
                    sin = -(float)Math.sin(angleB * Math.PI / 180.0);

                    v[n][0] = (r2 * cos);
                    v[n][1] = (h2);
                    v[n][2] = (r2 * sin);
                    v[n + 1][0] = (r1 * cos);
                    v[n + 1][1] = (h1);
                    v[n + 1][2] = (r1 * sin);

                    vBuf.put(v[n]);
                    vBuf.put(v[n + 1]);

                    n += 2;

                    if(n>31){
                        vBuf.position(0);

                        gl.glVertexPointer(3, GL10.GL_FLOAT, 0, vBuf);
                        gl.glNormalPointer(GL10.GL_FLOAT, 0, vBuf);
                        gl.glDrawArrays(GL10.GL_TRIANGLE_STRIP, 0, n);

                        n = 0;
                        angleB -= step;
                    }

                }
                vBuf.position(0);

                gl.glVertexPointer(3, GL10.GL_FLOAT, 0, vBuf);
                gl.glNormalPointer(GL10.GL_FLOAT, 0, vBuf);
                gl.glDrawArrays(GL10.GL_TRIANGLE_STRIP, 0, n);
            }

            gl.glDisableClientState(GL10.GL_VERTEX_ARRAY);
            gl.glDisableClientState(GL10.GL_NORMAL_ARRAY);
        }
        gl.glPopMatrix();
    }

    @Override
    public void update(float fracSec) {
        updatePosition(fracSec);

        //Wobble update
        if(wobbleX+0.015 <= 1.1 && wobbleXup){
            wobbleX+=0.015;
        }
        else{
            wobbleXup = false;
        }
        if(wobbleX-0.015 >= 0.9 && !wobbleXup){
            wobbleX-=0.015;
        }
        else{
            wobbleXup=true;
        }
        if(wobbleY+0.015 <= 1.1 && wobbleYup){
            wobbleY+=0.015;
        }
        else{
            wobbleYup = false;
        }
        if(wobbleY-0.015 >= 0.9 && !wobbleYup){
            wobbleY-=0.015;
        }
        else{
            wobbleYup=true;
        }
    }

    public BubbleColors getColor(){
        return color;
    }

}

