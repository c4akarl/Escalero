/*
    Escalero - An Android dice program.
    Copyright (C) 2016-2021  Karl Schreiner, c4akarl@gmail.com

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
import android.text.Editable
import android.view.View
import android.widget.*
import androidx.core.content.ContextCompat

class Preferences : Activity() {

    private lateinit var prefs: SharedPreferences
    internal var dice = 0   // 0 : result entry, 1 : dice
    private var isPlayOnline = true
    private var isPlayerColumn = false
    private var isDiceBoard = true
    private var diceSize = 2   // 1 small, 2 medium, 3 large
    private var icons = 1

    private lateinit var cbLogging: CheckBox
    private lateinit var cbMainDialog: CheckBox
    private lateinit var diceIcon1: ImageView
    private lateinit var diceIcon2: ImageView
    private lateinit var diceIcon3: ImageView
    private lateinit var col1: TextView
    private lateinit var col2: TextView
    private lateinit var col3: TextView
    private lateinit var bon: TextView
    private lateinit var multiplier: TextView
    private lateinit var unit: TextView
    private lateinit var bonusServed: TextView
    private lateinit var bonusServedGrande: TextView
    private lateinit var cbSummation: CheckBox
    private lateinit var cbNewGame: CheckBox
    private lateinit var cbSounds: CheckBox
    private lateinit var cbFlipScreen: CheckBox
    private lateinit var cbAdvertising: CheckBox
    private lateinit var rbDiceManuel: RadioButton
    private lateinit var rbDiceAutomatic: RadioButton
    private lateinit var rbSizeSmall: RadioButton
    private lateinit var rbSizeMedium: RadioButton
    private lateinit var rbSizeLarge: RadioButton
    private lateinit var rbAccountingPlayer: RadioButton
    private lateinit var rbAccountingColumns: RadioButton
    private lateinit var rbDimension3D: RadioButton
    private lateinit var rbDimension2D: RadioButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.prefs)
        cbLogging = findViewById<View>(R.id.cbLogging) as CheckBox
        cbMainDialog = findViewById<View>(R.id.cbMainDialog) as CheckBox
        diceIcon1 = findViewById<View>(R.id.dice_icon_1) as ImageView
        diceIcon2 = findViewById<View>(R.id.dice_icon_2) as ImageView
        diceIcon3 = findViewById<View>(R.id.dice_icon_3) as ImageView
        col1 = findViewById<View>(R.id.col1) as TextView
        col2 = findViewById<View>(R.id.col2) as TextView
        col3 = findViewById<View>(R.id.col3) as TextView
        bon = findViewById<View>(R.id.bon) as TextView
        multiplier = findViewById<View>(R.id.multiplier) as TextView
        unit = findViewById<View>(R.id.unit) as TextView
        bonusServed = findViewById<View>(R.id.bonusServed) as TextView
        bonusServedGrande = findViewById<View>(R.id.bonusServedGrande) as TextView
        cbSummation = findViewById<View>(R.id.cbSummation) as CheckBox
        cbNewGame = findViewById<View>(R.id.cbNewGame) as CheckBox
        cbSounds = findViewById<View>(R.id.cbSounds) as CheckBox
        cbFlipScreen = findViewById<View>(R.id.cbFlipScreen) as CheckBox
        cbAdvertising = findViewById<View>(R.id.cbAdvertising) as CheckBox
        rbDiceManuel = findViewById<View>(R.id.rbDiceManuel) as RadioButton
        rbDiceAutomatic = findViewById<View>(R.id.rbDiceAutomatic) as RadioButton
        rbSizeSmall = findViewById<View>(R.id.rbSizeSmall) as RadioButton
        rbSizeMedium = findViewById<View>(R.id.rbSizeMedium) as RadioButton
        rbSizeLarge = findViewById<View>(R.id.rbSizeLarge) as RadioButton
        rbAccountingPlayer = findViewById<View>(R.id.rbAccountingPlayer) as RadioButton
        rbAccountingColumns = findViewById<View>(R.id.rbAccountingColumns) as RadioButton
        rbDimension3D = findViewById<View>(R.id.rbDimension3D) as RadioButton
        rbDimension2D = findViewById<View>(R.id.rbDimension2D) as RadioButton

        prefs = getSharedPreferences("prefs", 0)

        isPlayOnline = prefs.getBoolean("playOnline", true)

        cbLogging!!.isChecked = prefs.getBoolean("logging", false)
        cbMainDialog!!.isChecked = prefs.getBoolean("mainDialog", true)
        dice = prefs.getInt("dice", 1)
        isDiceBoard = prefs.getBoolean("isDiceBoard", true)
        diceSize = prefs.getInt("diceSize", 2)
        icons = prefs.getInt("icons", 1)
        if (prefs.getInt("icons", 1) == 1)
            diceIcon1.setImageBitmap(getHoldBitmap(R.drawable._1_4, true))
        if (prefs.getInt("icons", 1) == 2)
            diceIcon2.setImageBitmap(getHoldBitmap(R.drawable._2_4, true))
        if (prefs.getInt("icons", 1) == 3)
            diceIcon3.setImageBitmap(getHoldBitmap(R.drawable._3_4, true))
        col1.setText(String.format("%d", prefs.getInt("pointsCol1", 1)))
        col2.setText(String.format("%d", prefs.getInt("pointsCol2", 2)))
        col3.setText(String.format("%d", prefs.getInt("pointsCol3", 4)))
        bon.setText(String.format("%d", prefs.getInt("pointsBon", 3)))
        multiplier.setText(String.format("%d", prefs.getInt("multiplier", 1)))
        unit.setText(prefs.getString("unit", resources.getString(R.string.points)))
        bonusServed.setText(String.format("%d", prefs.getInt("bonusServed", 5)))
        bonusServedGrande.setText(String.format("%d", prefs.getInt("bonusServedGrande", 30)))
        isPlayerColumn = prefs.getBoolean("isPlayerColumn", true)
        cbSummation!!.isChecked = prefs.getBoolean("isSummation", false)
        cbSounds!!.isChecked = prefs.getBoolean("sounds", true)
        cbFlipScreen!!.isChecked = prefs.getBoolean("computeFlipScreen", false)
        cbAdvertising!!.isChecked = prefs.getBoolean("advertising", true)
        cbNewGame!!.isChecked = false
        if (isPlayOnline)
            cbNewGame.visibility = CheckBox.INVISIBLE
        if (dice == 0)
            rbDiceManuel.isChecked = true
        if (dice == 1)
            rbDiceAutomatic.isChecked = true
        if (diceSize == 1)
            rbSizeSmall.isChecked = true
        if (diceSize == 2)
            rbSizeMedium.isChecked = true
        if (diceSize == 3)
            rbSizeLarge.isChecked = true
        if (isPlayerColumn)
            rbAccountingPlayer.isChecked = true
        else
            rbAccountingColumns.isChecked = true
        if (isDiceBoard)
            rbDimension3D.isChecked = true
        else
            rbDimension2D.isChecked = true
        var msg = getString(R.string.player)
        msg = msg.replace("0", "")
        msg = msg.replace(" ", "")
        rbAccountingPlayer.text = Editable.Factory.getInstance().newEditable(msg)

        if (isPlayOnline) {
            col1.isClickable = false
            col1.isFocusable = false
            col2.isClickable = false
            col2.isFocusable = false
            col3.isClickable = false
            col3.isFocusable = false
            bon.isClickable = false
            bon.isFocusable = false
            multiplier.isClickable = false
            multiplier.isFocusable = false
            unit.isClickable = false
            unit.isFocusable = false
            bonusServed.isClickable = false
            bonusServed.isFocusable = false
            bonusServedGrande.isClickable = false
            bonusServedGrande.isFocusable = false
        }

        //karl
        cbMainDialog.visibility = View.INVISIBLE
        cbAdvertising.visibility = View.INVISIBLE

    }

    fun onRadioButtonClicked(view: View) {
        val checked = view as RadioButton
        if (rbDiceManuel == checked)            {
            dice = 0
            if (isPlayOnline) {
                dice = 1
                rbDiceManuel.isChecked = false
                rbDiceAutomatic.isChecked = true
                Toast.makeText(applicationContext, getString(R.string.disabledOnline), Toast.LENGTH_SHORT).show()
            }
        }
        if (rbDiceAutomatic == checked)         { dice = 1 }
        if (rbAccountingPlayer == checked)      { isPlayerColumn = true }
        if (rbAccountingColumns == checked)     { isPlayerColumn = false }
        if (rbDimension3D == checked)           { isDiceBoard = true }
        if (rbDimension2D == checked)           { isDiceBoard = false }
        if (rbSizeSmall == checked)             { diceSize = 1 }
        if (rbSizeMedium == checked)            { diceSize = 2 }
        if (rbSizeLarge == checked)             { diceSize = 3 }
    }

    fun myClickHandler(view: View) {
        val returnIntent: Intent
        when (view.id) {
            R.id.btnOk -> {
                setPrefs()
                returnIntent = Intent()
                setResult(RESULT_OK, returnIntent)
                finish()
            }
            R.id.dice_icon_1 -> {
                icons = 1
                diceIcon1.setImageBitmap(getHoldBitmap(R.drawable._1_4, true))
                diceIcon2.setImageBitmap(getHoldBitmap(R.drawable._2_4, false))
                diceIcon3.setImageBitmap(getHoldBitmap(R.drawable._3_4, false))
            }
            R.id.dice_icon_2 -> {
                icons = 2
                diceIcon2.setImageBitmap(getHoldBitmap(R.drawable._2_4, true))
                diceIcon1.setImageBitmap(getHoldBitmap(R.drawable._1_4, false))
                diceIcon3.setImageBitmap(getHoldBitmap(R.drawable._3_4, false))
            }
            R.id.dice_icon_3 -> {
                icons = 3
                diceIcon3.setImageBitmap(getHoldBitmap(R.drawable._3_4, true))
                diceIcon1.setImageBitmap(getHoldBitmap(R.drawable._1_4, false))
                diceIcon2.setImageBitmap(getHoldBitmap(R.drawable._2_4, false))
            }
        }
    }

    private fun getHoldBitmap(imageId: Int, isDrawRect: Boolean): Bitmap {
        // hold BORDER
        val bm = BitmapFactory.decodeResource(this.resources, imageId).copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas()
        canvas.setBitmap(bm)
        val paint = Paint()
        val stroke = canvas.width / 6
        paint.strokeWidth = stroke.toFloat()
        paint.color = ContextCompat.getColor(this, R.color.colorHold)
        paint.style = Paint.Style.STROKE
        val width = canvas.width.toFloat()
        val height = canvas.height.toFloat()
        if (isDrawRect)
            canvas.drawRect(0f, 0f, width, height, paint)
        return bm
    }

    private fun setPrefs() {
        val ed = prefs.edit()

        ed.putInt("dice", dice)
        ed.putBoolean("isDiceBoard", isDiceBoard)
        ed.putInt("diceSize", diceSize)
        ed.putInt("icons", icons)

        ed.putBoolean("isPlayerColumn", isPlayerColumn)

        if (!col1.text.isNullOrEmpty())
            ed.putInt("pointsCol1", Integer.parseInt(col1.text.toString()))
        if (!col2.text.isNullOrEmpty())
            ed.putInt("pointsCol2", Integer.parseInt(col2.text.toString()))
        if (!col3.text.isNullOrEmpty())
            ed.putInt("pointsCol3", Integer.parseInt(col3.text.toString()))
        if (!bon.text.isNullOrEmpty())
            ed.putInt("pointsBon", Integer.parseInt(bon.text.toString()))
        if (!multiplier.text.isNullOrEmpty())
            ed.putInt("multiplier", Integer.parseInt(multiplier.text.toString()))
        if (unit.text != null)
            ed.putString("unit", unit.text.toString())
        if (!bonusServed.text.isNullOrEmpty())
            ed.putInt("bonusServed", Integer.parseInt(bonusServed.text.toString()))
        if (!bonusServedGrande.text.isNullOrEmpty())
            ed.putInt("bonusServedGrande", Integer.parseInt(bonusServedGrande.text.toString()))
        if (cbSummation != null)
            ed.putBoolean("isSummation", cbSummation!!.isChecked)
        if (cbSounds != null)
            ed.putBoolean("sounds", cbSounds!!.isChecked)
        if (cbFlipScreen != null) {
            if (isPlayOnline)
                ed.putBoolean("computeFlipScreen", false)
            else
                ed.putBoolean("computeFlipScreen", cbFlipScreen!!.isChecked)
        }
        if (cbLogging != null)
            ed.putBoolean("logging", cbLogging!!.isChecked)

        ed.putBoolean("mainDialog", false)
        ed.putBoolean("advertising", true)

        if (cbNewGame != null)
            ed.putBoolean("cbNewGame", cbNewGame!!.isChecked)

        ed.apply()

        var col1 = prefs.getInt("pointsCol1", 1)
        var col2 = prefs.getInt("pointsCol2", 2)
        var col3 = prefs.getInt("pointsCol3", 4)
        for (i in 0..1) {
            if (col1 > col2) {
                val tmp = col2
                col2 = col1
                col1 = tmp
            }
            if (col2 > col3) {
                val tmp = col3
                col3 = col2
                col2 = tmp
            }
        }
        ed.putInt("pointsCol1", col1)
        ed.putInt("pointsCol2", col2)
        ed.putInt("pointsCol3", col3)
        ed.apply()
    }

    companion object {
        const val TAG = "Preferences"
    }
}
