package de.spicysources.bubblepenetration;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.opengl.GLU;
import android.view.MotionEvent;
import de.spicysources.bubblepenetration.objects.Bubble;
import de.spicysources.bubblepenetration.objects.GameObject;
import de.spicysources.bubblepenetration.util.BubbleColors;
import de.spicysources.bubblepenetration.util.EffectTask;
import de.spicysources.bubblepenetration.util.Generator;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;
import javax.microedition.khronos.opengles.GL11;
import java.util.ArrayList;

public class BubbleGLSurfaceView extends GLSurfaceView {

    private boolean alarmed = false;
    private BubbleRenderer renderer;
    public Context context;  // activity context
    private Generator generator;

    public float boundaryTop, boundaryBottom, boundaryLeft, boundaryRight;

    private BubbleColors collectColor;
    private float timer;
    private int score;
    private boolean isTouch = false;

    private ArrayList<GameObject> gameObjects = new ArrayList<>();
    private ArrayList<GameObject> objectsToBeRemoved = new ArrayList<>();
    private ArrayList<GameObject> targetsToBeRemoved = new ArrayList<>();

    // game balance factors
    private int starScore=5 , starTime=5, bubbleScore=2, bubbleTime=2;
    private float increaseSpeed = 0.02f;

    // sound
    private EffectTask effectPlayer;

    public BubbleGLSurfaceView(Context context, boolean muted) {
        super(context);
        renderer = new BubbleRenderer();
        setRenderer(renderer);
        setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
        generator = new Generator();
        collectColor = BubbleColors.RED;
        score = 0;
        timer = 40.0f;
        this.effectPlayer = new EffectTask(context, muted);
        this.effectPlayer.start();
    }

    //Collect Bubbles
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction() & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN:
                //Checks if Object is hit by touch
                //isTouch locks remove loops in update method as long as the touch event is executet
                isTouch = true;
                int targetIndex = -1;
                int targetCounter = 0;
                double distance = 0.0;
                for( int i = 0; i < gameObjects.size(); i++ ) {
                    GameObject bubble = gameObjects.get(i);
                    float x = (event.getX() * renderer.getUnitsPerPixelX() - boundaryRight) - bubble.getX();
                    float y = ((event.getY() * renderer.getUnitsPerPixelZ() - boundaryTop) * -1) - bubble.getZ();

                    if (Math.sqrt(Math.pow(x, 2) + Math.pow(y, 2)) <= bubble.scale) {
                        if(distance <= 1E-20) {
                            distance = Math.sqrt(Math.pow(x, 2) + Math.pow(y, 2));
                            targetIndex = targetCounter;
                        }
                        else{
                            if(Math.sqrt(Math.pow(x, 2) + Math.pow(y, 2)) < distance){
                                distance = Math.sqrt(Math.pow(x, 2) + Math.pow(y, 2));
                                targetIndex = targetCounter;
                            }
                        }
                    }
                    targetCounter++;
                }
                if(targetIndex >= 0) {
                    targetsToBeRemoved.add(gameObjects.get(targetIndex));
                }
                isTouch = false;
        }
        return true;
    }

    private class BubbleRenderer implements Renderer {
        private float[] modelViewScene = new float[16];
        long lastFrameTime;
        private float unitsPerPixelX;
        private float unitsPerPixelZ;

        public BubbleRenderer() {
            lastFrameTime = System.currentTimeMillis();
        }

        @Override
        public void onDrawFrame(GL10 gl) {
            // update time calculation
            long delta = System.currentTimeMillis() - lastFrameTime;
            float fracSec = (float) delta / 1000;
            lastFrameTime = System.currentTimeMillis();

            // scene updates
            if(timer < 10 & !alarmed){
                effectPlayer.playSound(R.raw.alarm);
                alarmed = true;
            }
            if(timer - fracSec <= 0.0){
                timer = 0.0f;
            }
            else{
                timer -= fracSec;
            }
            // update color to collect
            collectColor = generator.generateCollectColor(collectColor, score);
            // refresh HUD
            ((de.spicysources.bubblepenetration.MainActivity)context).runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if(alarmed)
                        ((de.spicysources.bubblepenetration.MainActivity)context).timerBlink();
                    if(timer>10){
                        ((de.spicysources.bubblepenetration.MainActivity)context).setTimerAlpha(1.0f);
                        alarmed = false;
                    }
                    if(timer <= 0.0){
                        effectPlayer.setIngame(false);
                        ((de.spicysources.bubblepenetration.MainActivity)context).showGameover(""+score);
                    }
                    else{
                        ((de.spicysources.bubblepenetration.MainActivity)context).setTimerText("time: "+timeToTimeFormat(timer,1));
                        ((de.spicysources.bubblepenetration.MainActivity)context).setScoreText("score: "+score);
                        ((de.spicysources.bubblepenetration.MainActivity)context).setHUDColor(generator.getGLColor(collectColor));
                        //((MainActivity)context).setPreviewBubble(collectColor);
                    }
                }
            });
            //update gameobjects
            updateGameobjects(fracSec);
            // clear screen and depth buffer
            gl.glClear(GL10.GL_COLOR_BUFFER_BIT | GL10.GL_DEPTH_BUFFER_BIT);

            GL11 gl11 = (GL11) gl;

            // load local system to draw scene items
            gl.glMatrixMode(GL10.GL_MODELVIEW);
            gl11.glLoadMatrixf(modelViewScene, 0);
            //draw gameobjects
            for (GameObject object: gameObjects) {
                object.draw(gl);
            }
        }

        private void updateGameobjects(float fracSec) {
            // position update on all gameobjects
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
            for (GameObject object: targetsToBeRemoved) {
                if(isTouch){
                    break;
                }
                // collected bubble or star
                if(object instanceof Bubble) {
                    //Check if hit bubble have the right color
                    if (((Bubble) object).getColor() == collectColor) {
                        timer += bubbleTime;
                        score += bubbleScore;
                        GameObject.speed += increaseSpeed;
                        effectPlayer.playSound(R.raw.blubb);
                    } else {
                        timer -= bubbleScore*2;
                        effectPlayer.playSound(R.raw.fart);
                    }
                }
                else{
                    timer += starTime;
                    score += starScore;
                    GameObject.speed += increaseSpeed*2;
                    effectPlayer.playSound(R.raw.star);
                }
                gameObjects.remove(object);
            }
            targetsToBeRemoved.clear();
            // remove obsolete gameobjects
            for (GameObject object: objectsToBeRemoved) {
                if(isTouch){
                    break;
                }
                gameObjects.remove(object);
            }
            objectsToBeRemoved.clear();
            //spawn new gameobjects
            generator.generateGameobject( gameObjects, collectColor, boundaryBottom, boundaryTop, boundaryRight,boundaryLeft);
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
            float desired_height=10.0f;
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
            boundaryTop = desired_height/2;
            boundaryBottom = -desired_height/2;
            // x range is the desired width
            boundaryLeft = -(desired_height/2 * aspectRatio);
            boundaryRight = (desired_height/2 * aspectRatio);
            // tochevent pixel coordinates to openGL coordinates
            unitsPerPixelZ = desired_height/height;
            unitsPerPixelX = (desired_height * aspectRatio)/width;
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

        private double timeToTimeFormat(double time, int nachkommastellen){
            double factor = Math.pow(10,nachkommastellen);
            return ((double)((int)(time*factor)))/factor;
        }

        public float getUnitsPerPixelX(){
            return unitsPerPixelX;
        }

        public float getUnitsPerPixelZ(){
            return unitsPerPixelZ;
        }

    }
}

