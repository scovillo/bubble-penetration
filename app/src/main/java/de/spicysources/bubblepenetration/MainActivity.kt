package de.spicysources.bubblepenetration

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.StrictMode
import android.os.StrictMode.ThreadPolicy
import android.util.Log
import android.view.Display
import android.view.View
import android.view.WindowManager
import android.widget.*
import de.spicysources.bubblepenetration.database.DataConnection
import de.spicysources.bubblepenetration.objects.GameObject
import de.spicysources.bubblepenetration.screen.BubbleGLSurfaceView
import de.spicysources.bubblepenetration.screen.MenuGLSurfaceView
import java.io.*

class MainActivity : Activity() {

    private var blinkStep = 0.035f
    private var readFromFile = false
    private var musicMuted = false
    private var effectsMuted = false
    private var bubbleGLSurfaceView: BubbleGLSurfaceView? = null
    private var menuGLSurfaceView: MenuGLSurfaceView? = null
    private var mWindowManager: WindowManager? = null
    private var mDisplay: Display? = null
    private var timerText: TextView? = null
    private var scoreText: TextView? = null
    private var gameOverScore: TextView? = null
    private var title: TextView? = null
    private val filename = "bubblePenetration"
    private var username: String = "anonym"
    private var score: String? = null
    private var musicPlayer: MediaPlayer? = null

    fun startGame(view: View) {
        effectsMuted = !(findViewById<View>(R.id.effects_box) as CheckBox).isChecked
        mWindowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        mDisplay = mWindowManager!!.defaultDisplay
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setContentView(R.layout.game_hud)
        menuGLSurfaceView = null
        bubbleGLSurfaceView =
            BubbleGLSurfaceView(this, effectsMuted)
        val glSurfaceViewHolder = findViewById<View>(R.id.GLSurfaceViewHolder) as FrameLayout
        glSurfaceViewHolder.addView(bubbleGLSurfaceView)
        timerText = findViewById<View>(R.id.Timer) as TextView
        scoreText = findViewById<View>(R.id.Score) as TextView
        timerText!!.typeface = Typeface.createFromAsset(this.assets, "fonts/PLUMP.ttf")
        scoreText!!.typeface = Typeface.createFromAsset(this.assets, "fonts/PLUMP.ttf")
        startMusic()
    }

    fun showHighscores(view: View) {
        setContentView(R.layout.highscores)
        setNetwork()
        val table = findViewById<View>(R.id.highscore_table) as TableLayout
        (findViewById<View>(R.id.highscore_back_button) as Button).typeface = Typeface.createFromAsset(
            this.assets,
            "fonts/PLUMP.ttf"
        )
        (findViewById<View>(R.id.table_rank) as TextView).typeface = Typeface.createFromAsset(
            this.assets,
            "fonts/PLUMP.ttf"
        )
        (findViewById<View>(R.id.table_name) as TextView).typeface = Typeface.createFromAsset(
            this.assets,
            "fonts/PLUMP.ttf"
        )
        (findViewById<View>(R.id.table_score) as TextView).typeface = Typeface.createFromAsset(
            this.assets,
            "fonts/PLUMP.ttf"
        )
        val data = DataConnection.getHighscoreData(username);
        var i = 1
        while (i < data.size - 2) {
            val rank = generateHighscoreTextView()
            rank.text = data[i]
            rank.typeface = Typeface.createFromAsset(this.assets, "fonts/PLUMP.ttf")
            val name = generateHighscoreTextView()
            name.text = data[i + 1]
            name.typeface = Typeface.createFromAsset(this.assets, "fonts/PLUMP.ttf")
            val score = generateHighscoreTextView()
            score.text = data[i + 2]
            score.typeface = Typeface.createFromAsset(this.assets, "fonts/PLUMP.ttf")
            val row = TableRow(this)
            if (data[i] == "1") {
                rank.setTextColor(resources.getColor(Color.YELLOW))
                name.setTextColor(resources.getColor(Color.YELLOW))
                score.setTextColor(resources.getColor(Color.YELLOW))
            }
            if (data[i + 1] == username) {
                rank.setTextColor(Color.RED)
                name.setTextColor(Color.RED)
                score.setTextColor(Color.RED)
            }
            row.addView(rank)
            row.addView(name)
            row.addView(score)
            table.addView(row)
            i += 3
        }
    }

    private fun generateHighscoreTextView(): TextView {
        val tv = TextView(this)
        tv.layoutParams = TableRow.LayoutParams(
            TableRow.LayoutParams.WRAP_CONTENT,
            TableRow.LayoutParams.WRAP_CONTENT,
            0.25f
        )
        tv.gravity = 1
        tv.setTextColor(Color.WHITE)
        tv.textSize = 25f
        tv.textAlignment = View.TEXT_ALIGNMENT_CENTER
        return tv
    }

    fun backToMenu(view: View) {
        showMainMenu()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        readFromFile()
        if (!readFromFile) {
            showUsernameScreen(false)
        } else {
            showMainMenu()
        }
    }

    private fun showMainMenu() {
        setContentView(R.layout.activity_main)
        menuGLSurfaceView = MenuGLSurfaceView(this)
        val glSurfaceViewHolder = findViewById<View>(R.id.menuGLSurfaceViewHolder) as FrameLayout
        glSurfaceViewHolder.addView(menuGLSurfaceView)
        (findViewById<View>(R.id.menu_username) as TextView).text = username
        if (readFromFile) {
            setNetwork()
            DataConnection.getUsernameExists(username)
        }
        title = findViewById<View>(R.id.menu_title) as TextView
        (findViewById<View>(R.id.effects_box) as CheckBox).typeface = Typeface.createFromAsset(
            this.assets,
            "fonts/PLUMP.ttf"
        )
        (findViewById<View>(R.id.music_box) as CheckBox).typeface = Typeface.createFromAsset(
            this.assets,
            "fonts/PLUMP.ttf"
        )
        (findViewById<View>(R.id.menu_title) as TextView).typeface = Typeface.createFromAsset(
            this.assets,
            "fonts/PLUMP.ttf"
        )
        (findViewById<View>(R.id.menu_username) as TextView).typeface = Typeface.createFromAsset(
            this.assets,
            "fonts/PLUMP.ttf"
        )
        (findViewById<View>(R.id.start_button) as Button).typeface = Typeface.createFromAsset(
            this.assets,
            "fonts/PLUMP.ttf"
        )
        (findViewById<View>(R.id.highscore_button) as Button).typeface = Typeface.createFromAsset(
            this.assets,
            "fonts/PLUMP.ttf"
        )
        if (musicMuted) (findViewById<View>(R.id.music_box) as CheckBox).isChecked = false
        if (effectsMuted) (findViewById<View>(R.id.effects_box) as CheckBox).isChecked = false
        startMusic()
    }

    fun setGameOverScreen(score: String) {
        bubbleGLSurfaceView = null
        timerText = null
        GameObject.speed = 1.0f
        setContentView(R.layout.game_over)
        gameOverScore = findViewById<View>(R.id.your_score_textview) as TextView
        gameOverScore!!.text = score
        (findViewById<View>(R.id.game_over_textview) as TextView).typeface = Typeface.createFromAsset(
            this.assets,
            "fonts/PLUMP.ttf"
        )
        (findViewById<View>(R.id.your_score_textview) as TextView).typeface = Typeface.createFromAsset(
            this.assets,
            "fonts/PLUMP.ttf"
        )
        (findViewById<View>(R.id.your_score_label) as TextView).typeface = Typeface.createFromAsset(
            this.assets,
            "fonts/PLUMP.ttf"
        )
        (findViewById<View>(R.id.highscore_gameover) as Button).typeface = Typeface.createFromAsset(
            this.assets,
            "fonts/PLUMP.ttf"
        )
        (findViewById<View>(R.id.bewerten_button) as Button).typeface = Typeface.createFromAsset(
            this.assets,
            "fonts/PLUMP.ttf"
        )
        setNetwork()
        val better = DataConnection.putHighscoreData(username, score)
        if (better) {
            (findViewById<View>(R.id.highscore_label) as TextView).text = "Great! check your new rank!"
            (findViewById<View>(R.id.your_score_label) as TextView).text = "!!! New Highscore !!!"
            (findViewById<View>(R.id.your_score_label) as TextView).setTextColor(Color.RED)
        } else {
            (findViewById<View>(R.id.highscore_label) as TextView).text = "you were better...try again!"
            (findViewById<View>(R.id.your_score_label) as TextView).text = "Your score"
            (findViewById<View>(R.id.your_score_label) as TextView).setTextColor(Color.WHITE)
        }
        (findViewById<View>(R.id.highscore_label) as TextView).setTypeface(
            Typeface.createFromAsset(
                this.assets,
                "fonts/PLUMP.ttf"
            )
        )
        startMusic()
    }

    fun setTimerText(time: String?) {
        timerText!!.text = time
    }

    fun setScoreText(score: String?) {
        scoreText!!.text = score
    }

    fun titleBlink() {
        if (title != null && (title!!.alpha > 1 || title!!.alpha < 0.25f)) {
            blinkStep *= -1f
        }
        if (title != null) {
            title!!.alpha = title!!.alpha + blinkStep
        }
    }

    fun timerBlink() {
        if (timerText != null && (timerText!!.alpha > 1 || timerText!!.alpha < 0.05f)) {
            blinkStep *= -1f
        }
        if (timerText != null) {
            timerText!!.alpha = timerText!!.alpha + blinkStep * 2
        }
    }


    fun setTimerAlpha(value: Float) {
        if (timerText != null) {
            timerText!!.alpha = value
        }
    }

    public override fun onResume() {
        super.onResume()
        startMusic()
    }

    public override fun onPause() {
        super.onPause()
        GameObject.speed = 1.0f
        if (musicPlayer != null && musicPlayer!!.isPlaying) musicPlayer!!.pause()
    }

    fun writeToFile(view: View) {
        setNetwork()
        val data = (findViewById<View>(R.id.username_field) as EditText).text.toString()
        if (data.length < 11) {
            if (!DataConnection.getUsernameExists(data)) {
                try {
                    val outputStreamWriter = OutputStreamWriter(openFileOutput(filename, Context.MODE_PRIVATE))
                    outputStreamWriter.write(data)
                    outputStreamWriter.close()
                    username = data
                } catch (e: IOException) {
                    Log.e("Exception", "File write failed: $e")
                }
                showMainMenu()
            } else Toast.makeText(this, "username already exists!", Toast.LENGTH_SHORT).show()
        } else Toast.makeText(this, "sorry, maximal 10 letters!", Toast.LENGTH_SHORT).show()
    }

    private fun readFromFile() {
        try {
            val inputStream: InputStream? = openFileInput(filename)
            if (inputStream != null) {
                val inputStreamReader = InputStreamReader(inputStream)
                val bufferedReader = BufferedReader(inputStreamReader)
                var receiveString: String? = ""
                val stringBuilder = StringBuilder()
                while (bufferedReader.readLine().also { receiveString = it } != null) {
                    stringBuilder.append(receiveString)
                }
                inputStream.close()
                username = stringBuilder.toString()
                readFromFile = true
            }
        } catch (e: FileNotFoundException) {
            Log.e("login activity", "File not found: $e")
        } catch (e: IOException) {
            Log.e("login activity", "Can not read file: $e")
        }
    }

    fun setHUDColor(glCollectColor: FloatArray) {
        val max = 255
        findViewById<View>(R.id.hud).setBackgroundColor(
            Color.argb(
                (glCollectColor[3] * max).toInt(), (glCollectColor[0] * max).toInt(),
                (glCollectColor[1] * max).toInt(), (glCollectColor[2] * max).toInt()
            )
        )
    }

    private fun startMusic() {
        if (musicPlayer == null) {
            musicPlayer = MediaPlayer.create(this, R.raw.music)
            musicPlayer!!.isLooping = true
            musicPlayer!!.setVolume(0.3f, 0.3f)
        }
        if (!musicPlayer!!.isPlaying and !musicMuted) {
            musicPlayer!!.start()
        }
    }

    fun setMusic(view: View) {
        musicMuted = !(findViewById<View>(R.id.music_box) as CheckBox).isChecked
        if (musicMuted) {
            if (musicPlayer != null && musicPlayer!!.isPlaying) {
                musicPlayer!!.stop()
            }
            musicPlayer = null
        } else {
            startMusic()
        }
    }

    fun setEffects(view: View) {
        effectsMuted = !(findViewById<View>(R.id.effects_box) as CheckBox).isChecked
    }

    fun editUsername(view: View) {
        showUsernameScreen(true)
    }

    fun showUsernameScreen(isCancelEnabled: Boolean) {
        setContentView(R.layout.set_username)
        if (isCancelEnabled) {
            findViewById<View>(R.id.cancel).isEnabled = true
            findViewById<View>(R.id.cancel).visibility = View.VISIBLE
        } else {
            findViewById<View>(R.id.cancel).isEnabled = false
            findViewById<View>(R.id.cancel).visibility = View.INVISIBLE
        }
        (findViewById<View>(R.id.your_name) as TextView).typeface = Typeface.createFromAsset(
            this.assets,
            "fonts/PLUMP.ttf"
        )
        (findViewById<View>(R.id.warning) as TextView).typeface = Typeface.createFromAsset(
            this.assets,
            "fonts/PLUMP.ttf"
        )
        (findViewById<View>(R.id.save) as Button).typeface = Typeface.createFromAsset(
            this.assets,
            "fonts/PLUMP.ttf"
        )
        (findViewById<View>(R.id.cancel) as Button).typeface = Typeface.createFromAsset(
            this.assets,
            "fonts/PLUMP.ttf"
        )
        (findViewById<View>(R.id.username_field) as EditText).typeface = Typeface.createFromAsset(
            this.assets,
            "fonts/PLUMP.ttf"
        )
        (findViewById<View>(R.id.username_field) as EditText).setText(username)
    }

    private fun setNetwork() {
        if (Build.VERSION.SDK_INT > 9) {
            val policy = ThreadPolicy.Builder().permitAll().build()
            StrictMode.setThreadPolicy(policy)
        }
    }

    fun showGameover(score: String) {
        this.score = score
        setGameOverScreen(score)
    }

    fun launchMarket() {
        val uri = Uri.parse("market://details?id=$packageName")
        val myAppLinkToMarket = Intent(Intent.ACTION_VIEW, uri)
        try {
            startActivity(myAppLinkToMarket)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(this, " unable to find market app", Toast.LENGTH_LONG).show()
        }
    }

}