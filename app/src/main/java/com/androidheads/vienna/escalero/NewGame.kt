/*
    Escalero - An Android dice program.
    Copyright (C) 2016-2020  Karl Schreiner, c4akarl@gmail.com

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/

package com.androidheads.vienna.escalero

import android.app.Activity
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.os.Bundle
import android.os.Handler
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RadioButton
import androidx.core.content.ContextCompat
import kotlinx.android.synthetic.main.new_game.*
import java.util.*

class NewGame : Activity() {

    private var mUpdateRandom: Runnable = object : Runnable {
        override fun run() {
            when (playerStart) {
                'A' -> {
                    btnStartA.setImageResource(R.drawable.button_start)
                    btnStartB.setImageResource(R.drawable.button_start_grey)
                    btnStartC.setImageResource(R.drawable.button_start_grey)
                    playerStart = 'B'
                }
                'B' -> {
                    btnStartA.setImageResource(R.drawable.button_start_grey)
                    btnStartB.setImageResource(R.drawable.button_start)
                    btnStartC.setImageResource(R.drawable.button_start_grey)
                    playerStart = 'A'
                    if (players == 3)
                        playerStart = 'C'
                }
                'C' -> {
                    btnStartA.setImageResource(R.drawable.button_start_grey)
                    btnStartB.setImageResource(R.drawable.button_start_grey)
                    btnStartC.setImageResource(R.drawable.button_start)
                    playerStart = 'A'
                }
            }
            setPlayerMessage(playerStart)
            cntRandom++
            if (cntRandom >= 7) {
                setPlayerStart('?')
                btnOk.visibility = ImageView.VISIBLE

                handlerRandom.removeCallbacks(this)
            } else
                handlerRandom.postDelayed(this, 500)
        }
    }

    private lateinit var prefs: SharedPreferences
    private lateinit var runPrefs: SharedPreferences
    private lateinit var llPlayerC: LinearLayout

    private var isSingleGame = true
    internal var players = 2
    private var enginePlayer = false
    internal var enginePlayerA = false
    internal var enginePlayerB = false
    internal var enginePlayerC = false
    internal var playerA: String? = ""
    internal var playerB: String? = ""
    internal var playerC: String? = ""
    private var engineA: String? = ""
    private var engineB: String? = ""
    private var engineC: String? = ""
    internal var playerStart = 'A'

    var handlerRandom = Handler()
    internal var cntRandom = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.new_game)
        runPrefs = getSharedPreferences("run", 0)
        prefs = getSharedPreferences("prefs", 0)
        players = prefs.getInt("players", 2)

        btnChange.setOnClickListener { changePlayer() }
        btnRandom.setOnClickListener { randomPlayer() }
        btnOk.setOnClickListener{
            setPrefs()
            val returnIntent = Intent()
            setResult(RESULT_OK, returnIntent)
            finish()
        }

        btnHumanA.setOnClickListener { setPlayerABC('A', false) }
        btnMobileA.setOnClickListener { setPlayerABC('A', true) }
        nameA.addTextChangedListener(object : TextWatcher {
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                if ((s != "") and !enginePlayerA)
                    playerA = s.toString()
            }
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun afterTextChanged(s: Editable) {}
        })
        btnStartA.setOnClickListener { setPlayerStart('A') }

        btnHumanB.setOnClickListener { setPlayerABC('B', false) }
        btnMobileB.setOnClickListener { setPlayerABC('B', true) }
        nameB.addTextChangedListener(object : TextWatcher {
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                if ((s != "") and !enginePlayerB)
                    playerB = s.toString()
            }
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun afterTextChanged(s: Editable) {}
        })
        btnStartB.setOnClickListener { setPlayerStart('B') }

        llPlayerC = findViewById<View>(R.id.playerC) as LinearLayout
        btnHumanC.setOnClickListener { setPlayerABC('C', false) }
        btnMobileC.setOnClickListener { setPlayerABC('C', true) }
        nameC.addTextChangedListener(object : TextWatcher {
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                if ((s != "") and !enginePlayerC)
                    playerC = s.toString()
            }
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun afterTextChanged(s: Editable) {}
        })
        btnStartC.setOnClickListener { setPlayerStart('C') }

        setSingleDouble(prefs.getBoolean("isSingleGame", true))
        getPrefs()
        setPlayerABC('A', prefs.getBoolean("enginePlayerA", true))
        setPlayerABC('B', prefs.getBoolean("enginePlayerB", false))
        setPlayerABC('C', prefs.getBoolean("enginePlayerC", false))
        setPlayerNumber(players)
        setPlayerStart(runPrefs.getString("playerStart", "B")!![0])
        setEnginePlayer()

    }

    fun onRadioButtonClicked(view: View) {
        val checked = view as RadioButton
        if (rb_single == checked)         { setSingleDouble(true) }
        if (rb_double == checked)         { setSingleDouble(false) }
        if (rb_players2 == checked)         { setPlayerNumber(2) }
        if (rb_players3 == checked)         { setPlayerNumber(3) }
    }

    private fun setSingleDouble(isSingle: Boolean) {
        isSingleGame = isSingle
        if (isSingleGame) {
            rb_single.isChecked = true
            rb_players2.text = getString(R.string.playerNumber, 2)
            rb_players3.text = getString(R.string.playerNumber, 3)
        } else {
            rb_double.isChecked = true
            rb_players2.text = getString(R.string.team, 2)
            rb_players3.text = getString(R.string.team, 3)
        }
    }

    private fun setPlayerNumber(player: Int) {
        players = player
        if (players == 2) {
            rb_players2.isChecked = true
            llPlayerC.visibility = LinearLayout.INVISIBLE
            if (playerStart == 'C')
                setPlayerStart('?')
        }
        if (players == 3) {
            rb_players3.isChecked = true
            llPlayerC.visibility = LinearLayout.VISIBLE
        }
        if (isSingleGame) {
            rb_players2.text = getString(R.string.playerCnt, 2)
            rb_players3.text = getString(R.string.playerCnt, 3)
        }
        else {
            rb_players2.text = getString(R.string.team, 2)
            rb_players3.text = getString(R.string.team, 3)
        }
    }

    private fun setPlayerABC(player: Char, isEngine: Boolean) {
//        Log.i(TAG, "setPlayerABC(), playerName: " + playerName + ", isEngine: " + isEngine)
        when (player) {
            'A' -> {
                enginePlayerA = isEngine
                if (isEngine) {
                    btnMobileA.setImageBitmap(getHoldBitmap(R.drawable.button_mobile, true))
                    btnHumanA.setImageBitmap(getHoldBitmap(R.drawable.button_human_1, false))
                    nameA.isEnabled = false
                    nameA.setText(engineA)
                } else {
                    btnMobileA.setImageBitmap(getHoldBitmap(R.drawable.button_mobile, false))
                    btnHumanA.setImageBitmap(getHoldBitmap(R.drawable.button_human_1, true))
                    nameA.isEnabled = true
                    nameA.setText(playerA)
                }
            }
            'B' -> {
                enginePlayerB = isEngine
                if (isEngine) {
                    btnMobileB.setImageBitmap(getHoldBitmap(R.drawable.button_mobile, true))
                    btnHumanB.setImageBitmap(getHoldBitmap(R.drawable.button_human_1, false))
                    nameB.isEnabled = false
                    nameB.setText(engineB)
                } else {
                    btnMobileB.setImageBitmap(getHoldBitmap(R.drawable.button_mobile, false))
                    btnHumanB.setImageBitmap(getHoldBitmap(R.drawable.button_human_1, true))
                    nameB.isEnabled = true
                    nameB.setText(playerB)
                }
            }
            'C' -> {
                enginePlayerC = isEngine
                if (isEngine) {
                    btnMobileC.setImageBitmap(getHoldBitmap(R.drawable.button_mobile, true))
                    btnHumanC.setImageBitmap(getHoldBitmap(R.drawable.button_human_1, false))
                    nameC.isEnabled = false
                    nameC.setText(engineC)
                } else {
                    btnMobileC.setImageBitmap(getHoldBitmap(R.drawable.button_mobile, false))
                    btnHumanC.setImageBitmap(getHoldBitmap(R.drawable.button_human_1, true))
                    nameC.isEnabled = true
                    nameC.setText(playerC)
                }
            }
        }
        setPlayerMessage(playerStart)
    }

    private fun setEnginePlayer() {
        enginePlayer = false
        if (enginePlayerA)
            enginePlayer = true
        if (enginePlayerB)
            enginePlayer = true
        if (enginePlayerC and (players == 3))
            enginePlayer = true
    }

    private fun getHoldBitmap(imageId: Int, isDrawRect: Boolean): Bitmap {
        val bm = BitmapFactory.decodeResource(this.resources, imageId).copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas()
        canvas.setBitmap(bm)
        val paint = Paint()
        val stroke = canvas.width / 4
        paint.strokeWidth = stroke.toFloat()
        paint.color = ContextCompat.getColor(this, R.color.colorHoldNewGame)
        paint.style = Paint.Style.STROKE
        val width = canvas.width.toFloat()
        val height = canvas.height.toFloat()
        if (isDrawRect)
            canvas.drawRect(0f, 0f, width, height, paint)
        return bm
    }

    private fun changePlayer() {
        val saveEnginePlayerA = enginePlayerA
        val saveEnginePlayerB = enginePlayerB
        val saveEnginePlayerC = enginePlayerC
        if ((players == 2) and saveEnginePlayerA and saveEnginePlayerB)
            return
        if ((players == 3) and saveEnginePlayerA and saveEnginePlayerB and saveEnginePlayerC)
            return

        val saveNameA = playerA
        val saveNameB = playerB
        val saveNameC = playerC
        if (players == 2) {
            enginePlayerB = enginePlayerA
            enginePlayerA = saveEnginePlayerB
            playerA = saveNameB
            playerB = saveNameA
        }
        if (players == 3) {
            enginePlayerC = enginePlayerB
            enginePlayerB = enginePlayerA
            enginePlayerA = saveEnginePlayerC
            playerA = saveNameC
            playerB = saveNameA
            playerC = saveNameB
        }
        setPlayerABC('A', enginePlayerA)
        setPlayerABC('B', enginePlayerB)
        if (players == 3)
            setPlayerABC('C', enginePlayerC)
    }

    private fun randomPlayer() {
        cntRandom = 0
        btnOk.visibility = ImageView.INVISIBLE
        handlerRandom.postDelayed(mUpdateRandom, 0)
    }

    fun setPlayerStart(player: Char) {
        var pl = ""
        when (player) {
            'A' -> pl = "A"
            'B' -> pl = "B"
            'C' -> pl = "C"
            '?' -> pl = getPlayerStart(players)
        }
        playerStart = pl[0]
        if ((playerStart == 'C') and (players == 2))
            playerStart = 'A'

        when (playerStart) {
            'A' -> {
                btnStartA.setImageResource(R.drawable.button_start)
                btnStartB.setImageResource(R.drawable.button_start_grey)
                btnStartC.setImageResource(R.drawable.button_start_grey)
            }
            'B' -> {
                btnStartA.setImageResource(R.drawable.button_start_grey)
                btnStartB.setImageResource(R.drawable.button_start)
                btnStartC.setImageResource(R.drawable.button_start_grey)
            }
            'C' -> {
                btnStartA.setImageResource(R.drawable.button_start_grey)
                btnStartB.setImageResource(R.drawable.button_start_grey)
                btnStartC.setImageResource(R.drawable.button_start)
            }
        }
        setPlayerMessage(playerStart)
    }

    private fun setPlayerMessage(player: Char) {
        when (player) {
            'A' -> {
                playerMessage.text = getString(R.string.begins, nameA.text, player)
            }
            'B' -> {
                playerMessage.text = getString(R.string.begins, nameB.text, player)
            }
            'C' -> {
                playerMessage.text = getString(R.string.begins, nameC.text, player)
            }
        }
    }

    private fun getPlayerStart(players: Int): String {
        var startPlayer = "A"
        val rand = Random()
        val r = rand.nextInt(players)
        if (r == 0) startPlayer = "A"
        if (r == 1) startPlayer = "B"
        if (r == 2) startPlayer = "C"
        return startPlayer
    }

    private fun getPrefs() {
        playerA = prefs.getString("nameA", "")
        playerB = prefs.getString("nameB", "")
        playerC = prefs.getString("nameC", "")
        engineA = runPrefs.getString("engineNameA", "")
        engineB = runPrefs.getString("engineNameB", "")
        engineC = runPrefs.getString("engineNameC", "")
    }

    private fun setPrefs() {

        val ed = prefs.edit()
        ed.putBoolean("isSingleGame", isSingleGame)
        ed.putInt("players", players)
        setEnginePlayer()
        ed.putBoolean("enginePlayer", enginePlayer)
        ed.putBoolean("enginePlayerA", enginePlayerA)
        ed.putBoolean("enginePlayerB", enginePlayerB)
        ed.putBoolean("enginePlayerC", enginePlayerC)
        if (!enginePlayerA)
            ed.putString("nameA", nameA.text.toString())
        if (!enginePlayerB)
            ed.putString("nameB", nameB.text.toString())
        if (!enginePlayerC)
            ed.putString("nameC", nameC.text.toString())
        ed.putBoolean("gameFromFile", false)
        ed.apply()

        val edi = runPrefs.edit()
        edi.putString("playerStart", "" + playerStart)
        edi.apply()

    }

    companion object {
        const val TAG = "NewGame"
    }

}
