package de.spicysources.bubblepenetration.util;

import android.content.Context;
import android.media.MediaPlayer;
import de.spicysources.bubblepenetration.R;

import java.util.ArrayList;
import java.util.HashMap;

public class EffectTask extends Thread{

    private boolean muted = false, ingame = true;
    private ArrayList<Integer> sounds;
    private HashMap<Integer, MediaPlayer> effectPlayer;
    private Context context;

    public EffectTask(Context context, boolean muted){
        this.context = context;
        this.muted = muted;
        sounds = new ArrayList<>();
        effectPlayer = new HashMap<>();
        effectPlayer.put(R.raw.blubb, MediaPlayer.create(context, R.raw.blubb));
        effectPlayer.put(R.raw.fart, MediaPlayer.create(context, R.raw.fart));
        effectPlayer.put(R.raw.star, MediaPlayer.create(context, R.raw.star));
        effectPlayer.put(R.raw.alarm, MediaPlayer.create(context, R.raw.alarm));
    }

    @Override
    public void run(){
        while(ingame){
            if(!sounds.isEmpty()&!muted){
                if( effectPlayer.get(sounds.get(0)) == null )
                    effectPlayer.put(sounds.get(0), MediaPlayer.create(context, sounds.get(0)));
                effectPlayer.get(sounds.get(0)).start();
                sounds.remove(0);
            }
        }
    }

    public void playSound(int index){
        sounds.add(index);
    }

    public void setIngame(boolean ingame){
        this.ingame = ingame;
    }
}
