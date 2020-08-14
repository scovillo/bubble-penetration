package de.spicysources.bubblepenetration;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.opengl.GLU;
import de.spicysources.bubblepenetration.objects.GameObject;
import de.spicysources.bubblepenetration.util.BubbleColors;
import de.spicysources.bubblepenetration.util.Generator;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;
import javax.microedition.khronos.opengles.GL11;
import java.util.ArrayList;

public class MenuGLSurfaceView extends GLSurfaceView {

    private SpaceRenderer renderer;
    private Generator generator;
    private Context context;

    public float boundaryTop, boundaryBottom, boundaryLeft, boundaryRight;

    private ArrayList<GameObject> gameObjects = new ArrayList<>();
    private ArrayList<GameObject> objectsToBeRemoved = new ArrayList<>();

    public MenuGLSurfaceView(Context context) {
        super(context);
        this.context = context;
        renderer = new SpaceRenderer();
        setRenderer(renderer);
        setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
        generator = new Generator();
    }

    private class SpaceRenderer implements Renderer {
        private float[] modelViewScene = new float[16];

        long lastFrameTime;

        public SpaceRenderer() {
            lastFrameTime = System.currentTimeMillis();
        }

        @Override
        public void onDrawFrame(GL10 gl) {
            // update time calculation
            long delta = System.currentTimeMillis() - lastFrameTime;
            float fracSec = (float) delta / 1000;
            lastFrameTime = System.currentTimeMillis();
            // scene updates
            updateGameobjects(fracSec);
            // clear screen and depth buffer
            gl.glClear(GL10.GL_COLOR_BUFFER_BIT | GL10.GL_DEPTH_BUFFER_BIT);

            GL11 gl11 = (GL11) gl;

            // load local system to draw scene items
            gl.glMatrixMode(GL10.GL_MODELVIEW);
            gl11.glLoadMatrixf(modelViewScene, 0);
            for (GameObject object: gameObjects) {
                object.draw(gl);
            }
        }

        private void updateGameobjects(float fracSec) {
            ((de.spicysources.bubblepenetration.MainActivity)context).runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    ((de.spicysources.bubblepenetration.MainActivity)context).titleBlink();
                }
            });

            // position update on all obstacles
            for(GameObject object : gameObjects) {
                object.update(fracSec);
            }
            // check for gameobjects that flew out of the viewing area and remove
            // or deactivate them
            for (GameObject object : gameObjects) {
                // offset makes sure that the gameobjects don't get deleted or set
                // inactive while visible to the player.
                float offset = object.scale;
                if ((object.getX() > boundaryRight + offset)
                        || (object.getX() < boundaryLeft - offset)
                        || (object.getZ() > boundaryTop + offset)
                        || (object.getZ() < boundaryBottom - offset)) {
                    objectsToBeRemoved.add(object);
                }
            }
            // remove obsolete gameobjects
            for (GameObject object: objectsToBeRemoved) {
                gameObjects.remove(object);
            }
            objectsToBeRemoved.clear();
            //add new gameobjects
            generator.generateGameobject( gameObjects, BubbleColors.RED, boundaryBottom, boundaryTop, boundaryRight,boundaryLeft);
        }

        @Override
        // Called when surface is created or the viewport gets resized
        // set projection matrix
        // precalculate modelview matrix
        public void onSurfaceChanged(GL10 gl, int width, int height) {
            GL11 gl11 = (GL11) gl;
            gl.glViewport(0, 0, width, height);
            float aspectRatio = (float) width / height;
            float fovy = 45.0f;
            // set up projection matrix for scene
            gl.glMatrixMode(GL10.GL_PROJECTION);
            gl.glLoadIdentity();
            GLU.gluPerspective(gl, fovy, aspectRatio, 0.001f, 100.0f);
            // set up modelview matrix for scene
            gl.glMatrixMode(GL10.GL_MODELVIEW);
            gl.glLoadIdentity();
            float desired_height = 10.0f;
            // We want to be able to see the range of 5 to -5 units at the y
            // axis (height=10).
            // To achieve this we have to pull the camera towards the positive z axis
            // based on the following formula:
            // z = (desired_height / 2) / tan(fovy/2)
            float z = (float) (desired_height / 2 / Math.tan(fovy / 2 * (Math.PI / 180.0f)));
            // forward for the camera is backward for the scene
            gl.glTranslatef(0.0f, 0.0f, -z);
            // rotate local to achive top down view from negative y down to xz-plane
            // z range is the desired height
            gl.glRotatef(-90.0f, 1.0f, 0.0f, 0.0f);
            // save local system as a basis to draw scene items
            gl11.glGetFloatv(GL11.GL_MODELVIEW_MATRIX, modelViewScene, 0);
            // window boundaries
            // z range is the desired height
            boundaryTop = desired_height / 2;
            boundaryBottom = -desired_height / 2;
            // x range is the desired width
            boundaryLeft = -(desired_height / 2 * aspectRatio);
            boundaryRight = (desired_height / 2 * aspectRatio);
        }

        @Override
        public void onSurfaceCreated(GL10 gl, EGLConfig config) {
            gl.glDisable(GL10.GL_DITHER);
            gl.glHint(GL10.GL_PERSPECTIVE_CORRECTION_HINT, GL10.GL_FASTEST);
            gl.glBlendFunc(GL10.GL_SRC_ALPHA, GL10.GL_ONE_MINUS_SRC_ALPHA);
            gl.glEnable(GL10.GL_BLEND);
            gl.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
            gl.glEnable(GL10.GL_CULL_FACE);
            gl.glShadeModel(GL10.GL_FLAT);
            gl.glEnable(GL10.GL_DEPTH_TEST);
            gl.glDepthFunc(GL10.GL_LEQUAL);
            gl.glShadeModel(GL10.GL_SMOOTH);
            gl.glEnable(GL10.GL_DEPTH_TEST);
        }
    }

}

