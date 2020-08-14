package de.spicysources.bubblepenetration;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.StrictMode;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.view.WindowManager;
import android.widget.*;
import de.spicysources.bubblepenetration.database.DataConnection;
import de.spicysources.bubblepenetration.objects.GameObject;

import java.io.*;

public class MainActivity extends Activity {

    private float blinkStep = 0.035f;
    private boolean readFromFile = false, musicMuted = false, effectsMuted = false;
    private BubbleGLSurfaceView bubbleGLSurfaceView;
    private MenuGLSurfaceView menuGLSurfaceView;
    private WindowManager mWindowManager;
    private Display mDisplay;
    private TextView timerText, scoreText, gameOverScore, title;
    private String filename = "bubblePenetration", username = "anonym";
    private String score;
    private MediaPlayer musicPlayer;

    public void startGame(View v) {
        effectsMuted = !((CheckBox) findViewById(R.id.effects_box)).isChecked();
        mWindowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        mDisplay = mWindowManager.getDefaultDisplay();
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.game_hud);
        menuGLSurfaceView = null;
        bubbleGLSurfaceView = new BubbleGLSurfaceView(this, effectsMuted);
        bubbleGLSurfaceView.context = this;
        FrameLayout glSurfaceViewHolder = (FrameLayout) findViewById(R.id.GLSurfaceViewHolder);
        if (glSurfaceViewHolder != null) {
            glSurfaceViewHolder.addView(bubbleGLSurfaceView);
        }
        timerText = (TextView) findViewById(R.id.Timer);
        scoreText = (TextView) findViewById(R.id.Score);
        timerText.setTypeface(Typeface.createFromAsset(this.getAssets(), "fonts/PLUMP.ttf"));
        scoreText.setTypeface(Typeface.createFromAsset(this.getAssets(), "fonts/PLUMP.ttf"));
        startMusic();
    }

    public void showHighscores(View v) {
        setContentView(R.layout.highscores);
        setNetwork();
        TableLayout table = (TableLayout) findViewById(R.id.highscore_table);
        ((Button) findViewById(R.id.highscore_back_button)).setTypeface(Typeface.createFromAsset(this.getAssets(), "fonts/PLUMP.ttf"));
        ((TextView) findViewById(R.id.table_rank)).setTypeface(Typeface.createFromAsset(this.getAssets(), "fonts/PLUMP.ttf"));
        ((TextView) findViewById(R.id.table_name)).setTypeface(Typeface.createFromAsset(this.getAssets(), "fonts/PLUMP.ttf"));
        ((TextView) findViewById(R.id.table_score)).setTypeface(Typeface.createFromAsset(this.getAssets(), "fonts/PLUMP.ttf"));
        // TODO: Server Connection
        String[] data = new String[0]; //DataConnection.getHighscoreData(username);
        for (int i = 1; i < data.length - 2; i += 3) {
            TextView rank = generateHighscoreTextView();
            rank.setText(data[i]);
            rank.setTypeface(Typeface.createFromAsset(this.getAssets(), "fonts/PLUMP.ttf"));
            TextView name = generateHighscoreTextView();
            name.setText(data[i + 1]);
            name.setTypeface(Typeface.createFromAsset(this.getAssets(), "fonts/PLUMP.ttf"));
            TextView score = generateHighscoreTextView();
            score.setText(data[i + 2]);
            score.setTypeface(Typeface.createFromAsset(this.getAssets(), "fonts/PLUMP.ttf"));
            TableRow row = new TableRow(this);
            if (data[i].equals("1")) {
                rank.setTextColor(getResources().getColor(Color.YELLOW));
                name.setTextColor(getResources().getColor(Color.YELLOW));
                score.setTextColor(getResources().getColor(Color.YELLOW));
            }
            if (data[i + 1].equals(username)) {
                rank.setTextColor(Color.RED);
                name.setTextColor(Color.RED);
                score.setTextColor(Color.RED);
            }
            row.addView(rank);
            row.addView(name);
            row.addView(score);
            table.addView(row);
        }
    }

    private TextView generateHighscoreTextView() {
        TextView tv = new TextView(this);
        tv.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.WRAP_CONTENT, TableRow.LayoutParams.WRAP_CONTENT, 0.25f));
        tv.setGravity(1);
        tv.setTextColor(Color.WHITE);
        tv.setTextSize(25);
        tv.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        return tv;
    }

    public void backToMenu(View v) {
        showMainMenu();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        readFromFile();
        if (!readFromFile)
            setName();
        else
            showMainMenu();
    }

    private void showMainMenu() {
        setContentView(R.layout.activity_main);
        menuGLSurfaceView = new MenuGLSurfaceView(this);
        FrameLayout glSurfaceViewHolder = (FrameLayout) findViewById(R.id.menuGLSurfaceViewHolder);
        if (glSurfaceViewHolder != null) {
            glSurfaceViewHolder.addView(menuGLSurfaceView);
        }
        if (username != null) {
            ((TextView) findViewById(R.id.menu_username)).setText(username);
            if (readFromFile) {
                setNetwork();
                DataConnection.getUsernameExists(username);
            }
        }
        title = (TextView) findViewById(R.id.menu_title);
        ((CheckBox) findViewById(R.id.effects_box)).setTypeface(Typeface.createFromAsset(this.getAssets(), "fonts/PLUMP.ttf"));
        ((CheckBox) findViewById(R.id.music_box)).setTypeface(Typeface.createFromAsset(this.getAssets(), "fonts/PLUMP.ttf"));
        ((TextView) findViewById(R.id.menu_title)).setTypeface(Typeface.createFromAsset(this.getAssets(), "fonts/PLUMP.ttf"));
        ((TextView) findViewById(R.id.menu_username)).setTypeface(Typeface.createFromAsset(this.getAssets(), "fonts/PLUMP.ttf"));
        ((Button) findViewById(R.id.start_button)).setTypeface(Typeface.createFromAsset(this.getAssets(), "fonts/PLUMP.ttf"));
        ((Button) findViewById(R.id.highscore_button)).setTypeface(Typeface.createFromAsset(this.getAssets(), "fonts/PLUMP.ttf"));
        if (musicMuted)
            ((CheckBox) findViewById(R.id.music_box)).setChecked(false);
        if (effectsMuted)
            ((CheckBox) findViewById(R.id.effects_box)).setChecked(false);
        startMusic();
    }

    public void setGameOverScreen(String score) {
        bubbleGLSurfaceView = null;
        timerText = null;
        GameObject.speed = 1.0f;
        setContentView(R.layout.game_over);
        gameOverScore = (TextView) findViewById(R.id.your_score_textview);
        gameOverScore.setText(score);
        ((TextView) findViewById(R.id.game_over_textview)).setTypeface(Typeface.createFromAsset(this.getAssets(), "fonts/PLUMP.ttf"));
        ((TextView) findViewById(R.id.your_score_textview)).setTypeface(Typeface.createFromAsset(this.getAssets(), "fonts/PLUMP.ttf"));
        ((TextView) findViewById(R.id.your_score_label)).setTypeface(Typeface.createFromAsset(this.getAssets(), "fonts/PLUMP.ttf"));
        ((Button) findViewById(R.id.highscore_gameover)).setTypeface(Typeface.createFromAsset(this.getAssets(), "fonts/PLUMP.ttf"));
        ((Button) findViewById(R.id.bewerten_button)).setTypeface(Typeface.createFromAsset(this.getAssets(), "fonts/PLUMP.ttf"));
        setNetwork();
        boolean better = DataConnection.putHighscoreData(username, score);
        if (better) {
            ((TextView) findViewById(R.id.highscore_label)).setText("Great! check your new rank!");
            ((TextView) findViewById(R.id.your_score_label)).setText("!!! New Highscore !!!");
            ((TextView) findViewById(R.id.your_score_label)).setTextColor(Color.RED);
        } else {
            ((TextView) findViewById(R.id.highscore_label)).setText("you were better...try again!");
            ((TextView) findViewById(R.id.your_score_label)).setText("Your score");
            ((TextView) findViewById(R.id.your_score_label)).setTextColor(Color.WHITE);
        }
        ((TextView) findViewById(R.id.highscore_label)).setTypeface(Typeface.createFromAsset(this.getAssets(), "fonts/PLUMP.ttf"));
        startMusic();
    }

    public void setTimerText(String time) {
        timerText.setText(time);
    }

    public void setScoreText(String score) {
        scoreText.setText(score);
    }

    public void titleBlink() {
        if (title != null && title.getAlpha() > 1 | title.getAlpha() < 0.25) {
            blinkStep *= (-1);
        }
        if (title != null)
            title.setAlpha(title.getAlpha() + blinkStep);
    }

    public void timerBlink() {
        if (timerText != null && timerText.getAlpha() > 1 | timerText.getAlpha() < 0.05) {
            blinkStep *= (-1);
        }
        if (timerText != null)
            timerText.setAlpha(timerText.getAlpha() + blinkStep * 2);
    }

    public void setTimerAlpha(float value) {
        if (timerText != null)
            timerText.setAlpha(value);
    }

    @Override
    public void onResume() {
        super.onResume();
        startMusic();
    }

    @Override
    public void onPause() {
        super.onPause();
        GameObject.speed = 1.0f;
        if (musicPlayer != null && musicPlayer.isPlaying())
            musicPlayer.pause();
    }

    public void writeToFile(View v) {
        setNetwork();
        String data = ((EditText) findViewById(R.id.username_field)).getText().toString();
        if (data.length() < 11) {
            if (!DataConnection.getUsernameExists(data)) {
                try {
                    OutputStreamWriter outputStreamWriter = new OutputStreamWriter(this.openFileOutput(filename, Context.MODE_PRIVATE));
                    outputStreamWriter.write(data);
                    outputStreamWriter.close();
                    username = data;
                } catch (IOException e) {
                    Log.e("Exception", "File write failed: " + e.toString());
                }
                showMainMenu();
            } else
                Toast.makeText(this, "username already exists!", Toast.LENGTH_SHORT).show();
        } else
            Toast.makeText(this, "sorry, maximal 10 letters!", Toast.LENGTH_SHORT).show();
    }

    private void readFromFile() {
        try {
            InputStream inputStream = openFileInput(filename);

            if (inputStream != null) {
                InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
                BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
                String receiveString = "";
                StringBuilder stringBuilder = new StringBuilder();

                while ((receiveString = bufferedReader.readLine()) != null) {
                    stringBuilder.append(receiveString);
                }

                inputStream.close();
                username = stringBuilder.toString();
                readFromFile = true;
            }
        } catch (FileNotFoundException e) {
            Log.e("login activity", "File not found: " + e.toString());
        } catch (IOException e) {
            Log.e("login activity", "Can not read file: " + e.toString());
        }
    }

    public void setHUDColor(float[] glCollectColor) {
        int max = 255;
        (findViewById(R.id.hud)).setBackgroundColor(Color.argb(
                (int) (glCollectColor[3] * max), (int) (glCollectColor[0] * max),
                (int) (glCollectColor[1] * max), (int) (glCollectColor[2] * max)));
    }

    private void startMusic() {
        if (musicPlayer == null) {
            musicPlayer = MediaPlayer.create(this, R.raw.music);
            musicPlayer.setLooping(true);
            musicPlayer.setVolume(0.3f, 0.3f);
        }
        if (!musicPlayer.isPlaying() & !musicMuted)
            musicPlayer.start();
    }

    public void setMusic(View v) {
        musicMuted = !((CheckBox) findViewById(R.id.music_box)).isChecked();
        if (musicMuted) {
            if (musicPlayer != null && musicPlayer.isPlaying())
                musicPlayer.stop();
            musicPlayer = null;
        } else
            startMusic();
    }

    public void setEffects(View v) {
        effectsMuted = !((CheckBox) findViewById(R.id.effects_box)).isChecked();
    }

    public void setName(View v) {
        setContentView(R.layout.set_username);
        if (!findViewById(R.id.cancel).isEnabled()) {
            findViewById(R.id.cancel).setEnabled(true);
            findViewById(R.id.cancel).setVisibility(View.VISIBLE);
        }
        ((TextView) findViewById(R.id.your_name)).setTypeface(Typeface.createFromAsset(this.getAssets(), "fonts/PLUMP.ttf"));
        ((TextView) findViewById(R.id.warning)).setTypeface(Typeface.createFromAsset(this.getAssets(), "fonts/PLUMP.ttf"));
        ((Button) findViewById(R.id.save)).setTypeface(Typeface.createFromAsset(this.getAssets(), "fonts/PLUMP.ttf"));
        ((Button) findViewById(R.id.cancel)).setTypeface(Typeface.createFromAsset(this.getAssets(), "fonts/PLUMP.ttf"));
        ((EditText) findViewById(R.id.username_field)).setTypeface(Typeface.createFromAsset(this.getAssets(), "fonts/PLUMP.ttf"));
        ((EditText) findViewById(R.id.username_field)).setText(username);
    }

    private void setName() {
        setContentView(R.layout.set_username);
        findViewById(R.id.cancel).setEnabled(false);
        findViewById(R.id.cancel).setVisibility(View.INVISIBLE);
        ((TextView) findViewById(R.id.your_name)).setTypeface(Typeface.createFromAsset(this.getAssets(), "fonts/PLUMP.ttf"));
        ((TextView) findViewById(R.id.warning)).setTypeface(Typeface.createFromAsset(this.getAssets(), "fonts/PLUMP.ttf"));
        ((Button) findViewById(R.id.save)).setTypeface(Typeface.createFromAsset(this.getAssets(), "fonts/PLUMP.ttf"));
        ((Button) findViewById(R.id.cancel)).setTypeface(Typeface.createFromAsset(this.getAssets(), "fonts/PLUMP.ttf"));
        ((EditText) findViewById(R.id.username_field)).setTypeface(Typeface.createFromAsset(this.getAssets(), "fonts/PLUMP.ttf"));
        ((EditText) findViewById(R.id.username_field)).setText(username);
    }

    private void setNetwork() {
        if (android.os.Build.VERSION.SDK_INT > 9) {
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
            StrictMode.setThreadPolicy(policy);
        }
    }

    public void showGameover(String score) {
        this.score = score;
        setGameOverScreen(score);
    }

    public void launchMarket(View v) {
        Uri uri = Uri.parse("market://details?id=" + getPackageName());
        Intent myAppLinkToMarket = new Intent(Intent.ACTION_VIEW, uri);
        try {
            startActivity(myAppLinkToMarket);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, " unable to find market app", Toast.LENGTH_LONG).show();
        }
    }

}
