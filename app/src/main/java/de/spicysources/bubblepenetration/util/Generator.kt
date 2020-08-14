package de.spicysources.bubblepenetration.util;

import de.spicysources.bubblepenetration.objects.Bubble;
import de.spicysources.bubblepenetration.objects.GameObject;
import de.spicysources.bubblepenetration.objects.Star;

import java.util.HashMap;
import java.util.List;

import static de.spicysources.bubblepenetration.util.Utilities.normalize;

public class Generator {

    private HashMap<BubbleColors, float[]> colorCast = new HashMap<>();
    // number of possible objects on screen
    private int objectCount = 20;
    // Gameobject size border
    private float scale = 1.0f, minScale = 0.75f, maxScale = 0.9f;
    // factor for randomizing spawns: Star < randomSpawn < Bubble
    private float randomSpawn = 0.05f;
    float minSpawnDistanceBetweenObstacles = 1.5f;
    // delay for changing collect color
    private int delay = 6000; //[ms]
    private long timeFlag = System.currentTimeMillis() + delay;

    private void initColors(){
        //Map with possible bubblecolors and matching GL colors
        colorCast.put(BubbleColors.RED, new float[]{1.0f, 0.0f, 0.0f, 0.7f});
        colorCast.put(BubbleColors.ORANGE, new float[]{1.0f, 0.5f, 0.0f, 0.7f});
        colorCast.put(BubbleColors.YELLOW, new float[]{1.0f, 1.0f, 0.0f, 0.7f});
        colorCast.put(BubbleColors.GREEN, new float[]{0.0f, 1.0f, 0.0f, 0.7f});
        colorCast.put(BubbleColors.LIGHTBLUE, new float[]{0.0f, 1.0f, 1.0f, 0.7f});
        colorCast.put(BubbleColors.BLUE, new float[]{0.0f, 0.0f, 1.0f, 0.7f});
        colorCast.put(BubbleColors.PURPLE, new float[]{0.635f, 0.505f, 0.788f, 0.7f});
    }

    public Generator(){
        initColors();
    }

    public void generateGameobject(List<GameObject> gameObjects, BubbleColors collectColor,
                                   float boundaryBottom, float boundaryTop, float boundaryRight, float boundaryLeft)
    {
        // Spawn new bubble to match the target obstacle count
        if (objectCount > gameObjects.size()) {
            for (int i = 0; i < objectCount - gameObjects.size(); ++i) {
                // determine what kind of obstacle is spawned next
                scale = (float)Math.random() * (maxScale - minScale) + minScale;
                float spawnX = 0.0f;
                float spawnZ = 0.0f;
                float spawnOffset = scale * 0.5f;
                float velocity[] = new float[3];
                // determine source and destination quadrant
                int sourceCode = ((Math.random()<0.5?0:1)<<1) | (Math.random()<0.5?0:1);  // source quadrant
                int destCode = sourceCode ^ 3;	// destination quadrant is opposite of source
                //Log.d("Code", sourceCode+" "+destCode);

					/* sourceCode, destCode
					 * +----+----+
					 * | 00 | 01 |
					 * +----+----+
					 * | 10 | 11 |
					 * +----+----+
					 */

                // calculate source vertex position, <0.5 horizontal, else vertical
                if(Math.random()<0.5){  // horizontal placing, top or bottom
                    spawnZ = (sourceCode&2)>0?boundaryBottom-spawnOffset : boundaryTop+spawnOffset;
                    spawnX = (sourceCode&1)>0?boundaryRight*(float)Math.random() : boundaryLeft*(float)Math.random();
                }
                else{  // vertical placing, left or right
                    spawnZ = (sourceCode&2)>0?boundaryBottom*(float)Math.random() : boundaryTop*(float)Math.random();
                    spawnX = (sourceCode&1)>0?boundaryRight+spawnOffset : boundaryLeft-spawnOffset;
                }
                // calculate destination vertex position, <0.5 horizontal, else vertical
                if(Math.random()<0.5){  // horizontal placing, top or bottom
                    velocity[2] = (destCode&2)>0?boundaryBottom-spawnOffset : boundaryTop+spawnOffset;
                    velocity[0] = (destCode&1)>0?boundaryRight*(float)Math.random() : boundaryLeft*(float)Math.random();
                }
                else{  // vertical placing, left or right
                    velocity[2] = (destCode&2)>0?boundaryBottom*(float)Math.random() : boundaryTop*(float)Math.random();
                    velocity[0] = (destCode&1)>0?boundaryRight+spawnOffset : boundaryLeft-spawnOffset;
                }
                // calculate velocity
                velocity[0] -= spawnX;
                velocity[2] -= spawnZ;
                normalize(velocity);
                boolean positionOk = true;
                // check distance to other gameobjects
                for(GameObject object: gameObjects) {
                    float minDistance = 0.5f * scale + 0.5f * object.scale + minSpawnDistanceBetweenObstacles;
                    if(Math.abs(spawnX - object.getX()) < minDistance
                            && Math.abs(spawnZ - object.getZ()) < minDistance)
                        positionOk = false;	// Distance too small -> invalid position
                }
                if (!positionOk)
                    continue; // Invalid spawn position -> try again next time
                //Is the needed color available?
                boolean collectColorAvailable = false;
                for(GameObject object : gameObjects){
                    if(object instanceof Bubble && ((Bubble)object).getColor() == collectColor){
                        collectColorAvailable = true;
                        break;
                    }
                }
                //spawn new gameobject
                if(Math.random() <= randomSpawn){
                    Star newStar = new Star();
                    //stars a little bit smaller than Bubbles in average
                    newStar.scale = scale*0.85f;
                    newStar.setPosition(spawnX, 0, spawnZ);
                    newStar.velocity = velocity;
                    gameObjects.add(newStar);
                }
                else{
                    Bubble newBubble;
                    //make sure there is a bubble with color to collect
                    if(collectColorAvailable){
                        BubbleColors random = generateColor();
                        newBubble = new Bubble(random, colorCast.get(random));
                    }
                    else
                       newBubble = new Bubble(collectColor, colorCast.get(collectColor));
                    newBubble.scale = scale;
                    newBubble.setPosition(spawnX, 0, spawnZ);
                    newBubble.velocity = velocity;
                    gameObjects.add(newBubble);
                }
            }
        }
    }

    //Randomizes color value
    public BubbleColors generateColor(){
        return BubbleColors.values()[(int)(Math.random()*BubbleColors.values().length)];
    }

    //Randomizes color to be collected
    public BubbleColors generateCollectColor(BubbleColors currentColor, int score){
        BubbleColors collectColor = currentColor;
        // if time exceeds delay, change Color
        if(System.currentTimeMillis() >= timeFlag) {
            //make sure color is changing
            while(collectColor == currentColor)
                collectColor = generateColor();
            // more score means faster change, max at 300 score (4500[ms])
            float scoreScale = 1-score/400;
            if(scoreScale < 0.75f)
                scoreScale = 0.75f;
            timeFlag = System.currentTimeMillis() + (int)(delay*scoreScale);
        }
        return collectColor;
    }

    public float[] getGLColor(BubbleColors color){
        return colorCast.get(color);
    }
}