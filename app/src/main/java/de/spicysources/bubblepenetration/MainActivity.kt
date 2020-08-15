package de.spicysources.bubblepenetration

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.StrictMode
import android.view.View
import android.view.WindowManager
import android.widget.*
import de.spicysources.bubblepenetration.data.DataConnection
import de.spicysources.bubblepenetration.data.LocalFileStorage
import de.spicysources.bubblepenetration.objects.GameObject
import de.spicysources.bubblepenetration.screen.BubbleGLSurfaceView
import de.spicysources.bubblepenetration.screen.MenuGLSurfaceView
import de.spicysources.bubblepenetration.sound.MusicPlayer

class MainActivity : Activity() {

    private var areSoundEffectsMuted = false
    private var bubbleGLSurfaceView: BubbleGLSurfaceView? = null
    private var menuGLSurfaceView: MenuGLSurfaceView? = null
    private var mWindowManager: WindowManager? = null
    private val musicPlayer = MusicPlayer(this)
    private val localFileStorage = LocalFileStorage(this)

    private lateinit var username: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        musicPlayer.init()
        username = localFileStorage.readFromFile()
        if (username.isBlank()) {
            showUsernameScreen(false)
        } else {
            showMainMenu()
        }
    }

    public override fun onResume() {
        super.onResume()
        musicPlayer.start()
    }

    public override fun onPause() {
        super.onPause()
        GameObject.speed = 1.0f
        musicPlayer.pause()
    }

    fun startGame(view: View) {
        areSoundEffectsMuted = !(findViewById<View>(R.id.effects_box) as CheckBox).isChecked
        mWindowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setContentView(R.layout.game_hud)
        menuGLSurfaceView = null
        bubbleGLSurfaceView = BubbleGLSurfaceView(this)
        val glSurfaceViewHolder = findViewById<View>(R.id.GLSurfaceViewHolder) as FrameLayout
        glSurfaceViewHolder.addView(bubbleGLSurfaceView)
        bubbleGLSurfaceView!!.isMuted(areSoundEffectsMuted)
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

    fun showMainMenu() {
        setContentView(R.layout.activity_main)
        menuGLSurfaceView = MenuGLSurfaceView(this)
        val glSurfaceViewHolder = findViewById<View>(R.id.menuGLSurfaceViewHolder) as FrameLayout
        glSurfaceViewHolder.addView(menuGLSurfaceView)
        (findViewById<View>(R.id.menu_username) as TextView).text = username
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
        if (musicPlayer.isMuted) {
            (findViewById<View>(R.id.music_box) as CheckBox).isChecked = false
        }
        if (areSoundEffectsMuted) {
            (findViewById<View>(R.id.effects_box) as CheckBox).isChecked = false
        }
    }

    fun showGameOverScreen(score: String) {
        bubbleGLSurfaceView = null
        GameObject.speed = 1.0f
        setContentView(R.layout.game_over)
        val gameOverScore = findViewById<View>(R.id.your_score_textview) as TextView
        gameOverScore.text = score
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
    }

    fun setMusic(view: View) {
        val isMusicMuted = !(findViewById<View>(R.id.music_box) as CheckBox).isChecked
        musicPlayer.isMuted = isMusicMuted
    }

    fun setEffects(view: View) {
        areSoundEffectsMuted = !(findViewById<View>(R.id.effects_box) as CheckBox).isChecked
    }

    fun saveUsername(view: View) {
        val value = (findViewById<View>(R.id.username_field) as EditText).text.toString()
        if (value.isBlank()) {
            Toast.makeText(this, "sorry, username can not be empty!", Toast.LENGTH_SHORT).show()
            return
        }
        if (value.length > 10) {
            Toast.makeText(this, "sorry, maximal 10 letters!", Toast.LENGTH_SHORT).show()
            return
        }
        if (DataConnection.getUsernameExists(value)) {
            Toast.makeText(this, "username already exists!", Toast.LENGTH_SHORT).show()
            return
        }
        username = value
        localFileStorage.writeToFile(username)
        showMainMenu()
    }

    fun showUsernameScreen(view: View) {
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

    fun setNetwork() {
        if (Build.VERSION.SDK_INT > 9) {
            val policy = StrictMode.ThreadPolicy.Builder().permitAll().build()
            StrictMode.setThreadPolicy(policy)
        }
    }

    fun launchMarket(view: View) {
        val uri = Uri.parse("market://details?id=$packageName")
        val myAppLinkToMarket = Intent(Intent.ACTION_VIEW, uri)
        try {
            startActivity(myAppLinkToMarket)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(this, " unable to find market app", Toast.LENGTH_LONG).show()
        }
    }

}