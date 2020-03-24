/*
    Escalero - An Android dice program.
    Copyright (C) 2016-2019  Karl Schreiner, c4akarl@gmail.com

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

import android.content.Context
import android.content.SharedPreferences
import android.content.res.Configuration
import java.util.*
import kotlin.math.abs

open class EscaleroData(private val context: Context, runPrefs: SharedPreferences, prefs: SharedPreferences,
                   displayWidth: Int, displayHeight: Int, displayDensity: Float, displayOrientation: Int) {
    val gridValues: Array<String>
        get() {
            val gridViewValues = Array(gridCurrent.size) { "" }
            for (i in gridCurrent.indices) {
                gridViewValues[i] = ""
            }
            for (i in gridCurrent.indices) {
                if (gridCurrent[i].startsWith("A") or gridCurrent[i].startsWith("B")
                        or gridCurrent[i].startsWith("C") or gridCurrent[i].startsWith("S"))
                    gridViewValues[i] = getPlayerValueFromArray(gridCurrent[i])
                if (gridCurrent[i] == "LA0") gridViewValues[i] = LA0
                if (gridCurrent[i] == "LB0") gridViewValues[i] = LB0
                if (gridCurrent[i] == "LC0") gridViewValues[i] = LC0
                if (gridCurrent[i] == "LA1") gridViewValues[i] = LA1
                if (gridCurrent[i] == "LB1") gridViewValues[i] = LB1
                if (gridCurrent[i] == "LC1") gridViewValues[i] = LC1
                if (gridCurrent[i] == "LA2") gridViewValues[i] = LA2
                if (gridCurrent[i] == "LB2") gridViewValues[i] = LB2
                if (gridCurrent[i] == "LC2") gridViewValues[i] = LC2

                if (gridCurrent[i] == "T0") gridViewValues[i] = t0
                if (gridCurrent[i] == "T1") gridViewValues[i] = t1
                if (gridCurrent[i] == "T2") gridViewValues[i] = t2
                if (gridCurrent[i] == "T3") gridViewValues[i] = t3
                if (gridCurrent[i] == "T4") gridViewValues[i] = t4
                if (gridCurrent[i] == "T5") gridViewValues[i] = t5
                if (gridCurrent[i] == "T6") gridViewValues[i] = t6
                if (gridCurrent[i] == "T7") gridViewValues[i] = t7
                if (gridCurrent[i] == "T8") gridViewValues[i] = t8
                if (gridCurrent[i] == "T9") gridViewValues[i] = t9
                if (gridCurrent[i] == "N0") gridViewValues[i] = n0
                if (gridCurrent[i] == "N1") gridViewValues[i] = n1
                if (gridCurrent[i] == "N2") gridViewValues[i] = n2
                if (gridCurrent[i] == "N3") gridViewValues[i] = n3
                if (gridCurrent[i] == "N4") gridViewValues[i] = n4
                if (gridCurrent[i] == "N5") gridViewValues[i] = n5
                if (gridCurrent[i] == "N6") gridViewValues[i] = n6
                if (gridCurrent[i] == "N7") gridViewValues[i] = n7
                if (gridCurrent[i] == "N8") gridViewValues[i] = n8
                if (gridCurrent[i] == "N9") gridViewValues[i] = n9

                if (gridCurrent[i] == "X00") gridViewValues[i] = restMoves
                if (gridCurrent[i] == "X01") {
                    if (isFlipScreen)
                        gridViewValues[i] = X01F
                    else
                        gridViewValues[i] = X01N
                }
                if (gridCurrent[i] == "X02") {
                    if (isSummation)
                        gridViewValues[i] = X02S
                    else
                        gridViewValues[i] = X02D
                }
                if (gridCurrent[i] == "X03") gridViewValues[i] = X03
            }
            return gridViewValues
        }

    val numColumns: Int
        get() = if (playerNumber == 2)
            8
        else
            11

    val isNewGame: Boolean
        get() {
            var newGame = true
            for (i in 0 until COLS) {
                for (j in 0 until ROWS) {
                    if (resultsPlayerA[i][j] >= 0)
                        newGame = false
                    if (resultsPlayerB[i][j] >= 0)
                        newGame = false
                    if (playerNumber == 3) {
                        if (resultsPlayerC[i][j] >= 0)
                            newGame = false
                    }
                }
            }
            return newGame
        }

    val isGameOver: Boolean
        get() {
            for (i in 0 until COLS) {
                for (j in 0 until ROWS) {
                    if (resultsPlayerA[i][j] < 0)
                        return false
                    if (resultsPlayerB[i][j] < 0)
                        return false
                    if (playerNumber == 3) {
                        if (resultsPlayerC[i][j] < 0)
                            return false
                    }
                }
            }
            selectedGridItem = -1
            return true
        }

    //            Log.i(TAG, "playerNumber == 3");
    //            Log.i(TAG, "sumA0: " + sumA0 + ", sumB0: " + sumB0 + ", sumC0: " + sumC0);
    //            Log.i(TAG, "sumA1: " + sumA1 + ", sumB1: " + sumB1 + ", sumC1: " + sumC1);
    //            Log.i(TAG, "sumA2: " + sumA2 + ", sumB2: " + sumB2 + ", sumC2: " + sumC2);

    var resultA = 0.0
    var resultB = 0.0
    private var resultC = 0.0
    val accounting: String
        get() {
            var accounting: String
            val winCol0 = pointsColumn1 * payoutMultiplier
            val winCol1 = pointsColumn2 * payoutMultiplier
            val winCol2 = pointsColumn3 * payoutMultiplier
            val winBonus = pointsBonus * payoutMultiplier
            var sumA0 = 0
            var sumB0 = 0
            var sumC0 = 0
            var sumA1 = 0
            var sumB1 = 0
            var sumC1 = 0
            var sumA2 = 0
            var sumB2 = 0
            var sumC2 = 0

            resultA = 0.0
            resultB = 0.0
            resultC = 0.0
            var resultA0 = 0.0
            var resultB0 = 0.0
            var resultC0 = 0.0
            var resultA1 = 0.0
            var resultB1 = 0.0
            var resultC1 = 0.0
            var resultA2 = 0.0
            var resultB2 = 0.0
            var resultC2 = 0.0

            var strBonusA = ""
            var strBonusB = ""
            var strBonusC = ""

            for (j in 0 until ROWS) {
                if (resultsPlayerA[0][j] >= 0)
                    sumA0 += resultsPlayerA[0][j]
                if (resultsPlayerB[0][j] >= 0)
                    sumB0 += resultsPlayerB[0][j]
                if (resultsPlayerC[0][j] >= 0)
                    sumC0 += resultsPlayerC[0][j]
            }
            for (j in 0 until ROWS) {
                if (resultsPlayerA[1][j] >= 0)
                    sumA1 += resultsPlayerA[1][j]
                if (resultsPlayerB[1][j] >= 0)
                    sumB1 += resultsPlayerB[1][j]
                if (resultsPlayerC[1][j] >= 0)
                    sumC1 += resultsPlayerC[1][j]
            }
            for (j in 0 until ROWS) {
                if (resultsPlayerA[2][j] >= 0)
                    sumA2 += resultsPlayerA[2][j]
                if (resultsPlayerB[2][j] >= 0)
                    sumB2 += resultsPlayerB[2][j]
                if (resultsPlayerC[2][j] >= 0)
                    sumC2 += resultsPlayerC[2][j]
            }

            if (playerNumber == 2) {
                if (sumA0 != sumB0) {
                    if (sumA0 > sumB0) {
                        resultA0 = winCol0.toDouble()
                        resultB0 = (winCol0 * -1).toDouble()
                    } else {
                        resultA0 = (winCol0 * -1).toDouble()
                        resultB0 = winCol0.toDouble()
                    }
                }
                if (sumA1 != sumB1) {
                    if (sumA1 > sumB1) {
                        resultA1 = winCol1.toDouble()
                        resultB1 = (winCol1 * -1).toDouble()
                    } else {
                        resultA1 = (winCol1 * -1).toDouble()
                        resultB1 = winCol1.toDouble()
                    }
                }
                if (sumA2 != sumB2) {
                    if (sumA2 > sumB2) {
                        resultA2 = winCol2.toDouble()
                        resultB2 = (winCol2 * -1).toDouble()
                    } else {
                        resultA2 = (winCol2 * -1).toDouble()
                        resultB2 = winCol2.toDouble()
                    }
                }
                resultA = resultA0 + resultA1 + resultA2
                resultB = resultB0 + resultB1 + resultB2
                if (winBonus > 0) {
                    if ((resultA0 > 0) and (resultA1 > 0) and (resultA2 > 0)) {
                        strBonusA = ",$winBonus"
                        strBonusB = "," + winBonus * -1
                        resultA += winBonus
                        resultB -= winBonus
                    }
                    if ((resultB0 > 0) and (resultB1 > 0) and (resultB2 > 0)) {
                        strBonusB = ",$winBonus"
                        strBonusA = "," + winBonus * -1
                        resultA -= winBonus
                        resultB += winBonus
                    }
                }
            }
            if (playerNumber == 3) {

                if (!((sumA0 == sumB0) and (sumA0 == sumC0))) {
                    if ((sumA0 == sumB0) and (sumA0 > sumC0)) {
                        resultA0 = winCol0.toDouble() / 2
                        resultB0 = winCol0.toDouble() / 2
                        resultC0 = (winCol0 * -1).toDouble()
                    }
                    if ((sumA0 == sumC0) and (sumA0 > sumB0)) {
                        resultA0 = winCol0.toDouble() / 2
                        resultC0 = winCol0.toDouble() / 2
                        resultB0 = (winCol0 * -1).toDouble()
                    }
                    if ((sumB0 == sumC0) and (sumB0 > sumA0)) {
                        resultB0 = winCol0.toDouble() / 2
                        resultC0 = winCol0.toDouble() / 2
                        resultA0 = (winCol0 * -1).toDouble()
                    }
                    if ((sumA0 > sumB0) and (sumA0 > sumC0)) {
                        resultA0 = (winCol0 * 2).toDouble()
                        resultB0 = (winCol0 * -1).toDouble()
                        resultC0 = (winCol0 * -1).toDouble()
                    }
                    if ((sumB0 > sumA0) and (sumB0 > sumC0)) {
                        resultB0 = (winCol0 * 2).toDouble()
                        resultA0 = (winCol0 * -1).toDouble()
                        resultC0 = (winCol0 * -1).toDouble()
                    }
                    if ((sumC0 > sumA0) and (sumC0 > sumB0)) {
                        resultC0 = (winCol0 * 2).toDouble()
                        resultA0 = (winCol0 * -1).toDouble()
                        resultB0 = (winCol0 * -1).toDouble()
                    }
                }

                if (!((sumA1 == sumB1) and (sumA1 == sumC1))) {
                    if ((sumA1 == sumB1) and (sumA1 > sumC1)) {
                        resultA1 = winCol1.toDouble() / 2
                        resultB1 = winCol1.toDouble() / 2
                        resultC1 = (winCol1 * -1).toDouble()
                    }
                    if ((sumA1 == sumC1) and (sumA1 > sumB1)) {
                        resultA1 = winCol1.toDouble() / 2
                        resultC1 = winCol1.toDouble() / 2
                        resultB1 = (winCol1 * -1).toDouble()
                    }
                    if ((sumB1 == sumC1) and (sumB1 > sumA1)) {
                        resultB1 = winCol1.toDouble() / 2
                        resultC1 = winCol1.toDouble() / 2
                        resultA1 = (winCol1 * -1).toDouble()
                    }
                    if ((sumA1 > sumB1) and (sumA1 > sumC1)) {
                        resultA1 = (winCol1 * 2).toDouble()
                        resultB1 = (winCol1 * -1).toDouble()
                        resultC1 = (winCol1 * -1).toDouble()
                    }
                    if ((sumB1 > sumA1) and (sumB1 > sumC1)) {
                        resultB1 = (winCol1 * 2).toDouble()
                        resultA1 = (winCol1 * -1).toDouble()
                        resultC1 = (winCol1 * -1).toDouble()
                    }
                    if ((sumC1 > sumA1) and (sumC1 > sumB1)) {
                        resultC1 = (winCol1 * 2).toDouble()
                        resultA1 = (winCol1 * -1).toDouble()
                        resultB1 = (winCol1 * -1).toDouble()
                    }
                }

                if (!((sumA2 == sumB2) and (sumA2 == sumC2))) {
                    if ((sumA2 == sumB2) and (sumA2 > sumC2)) {
                        resultA2 = winCol2.toDouble() / 2
                        resultB2 = winCol2.toDouble() / 2
                        resultC2 = (winCol2 * -1).toDouble()
                    }
                    if ((sumA2 == sumC2) and (sumA2 > sumB2)) {
                        resultA2 = winCol2.toDouble() / 2
                        resultC2 = winCol2.toDouble() / 2
                        resultB2 = (winCol2 * -1).toDouble()
                    }
                    if ((sumB2 == sumC2) and (sumB2 > sumA2)) {
                        resultB2 = winCol2.toDouble() / 2
                        resultC2 = winCol2.toDouble() / 2
                        resultA2 = (winCol2 * -1).toDouble()
                    }
                    if ((sumA2 > sumB2) and (sumA2 > sumC2)) {
                        resultA2 = (winCol2 * 2).toDouble()
                        resultB2 = (winCol2 * -1).toDouble()
                        resultC2 = (winCol2 * -1).toDouble()
                    }
                    if ((sumB2 > sumA2) and (sumB2 > sumC2)) {
                        resultB2 = (winCol2 * 2).toDouble()
                        resultA2 = (winCol2 * -1).toDouble()
                        resultC2 = (winCol2 * -1).toDouble()
                    }
                    if ((sumC2 > sumA2) and (sumC2 > sumB2)) {
                        resultC2 = (winCol2 * 2).toDouble()
                        resultA2 = (winCol2 * -1).toDouble()
                        resultB2 = (winCol2 * -1).toDouble()
                    }
                }

                resultA = resultA0 + resultA1 + resultA2
                resultB = resultB0 + resultB1 + resultB2
                resultC = resultC0 + resultC1 + resultC2

                if (winBonus > 0) {
                    if ((resultA0 > 0) and (resultA1 > 0) and (resultA2 > 0)) {
                        strBonusA = "," + winBonus * 2
                        strBonusB = "," + winBonus * -1
                        strBonusC = "," + winBonus * -1
                        resultA += winBonus * 2
                        resultB -= winBonus
                        resultC -= winBonus
                    }
                    if ((resultB0 > 0) and (resultB1 > 0) and (resultB2 > 0)) {
                        strBonusB = "," + winBonus * 2
                        strBonusA = "," + winBonus * -1
                        strBonusC = "," + winBonus * -1
                        resultB += winBonus * 2
                        resultA -= winBonus
                        resultC -= winBonus
                    }
                    if ((resultC0 > 0) and (resultC1 > 0) and (resultC2 > 0)) {
                        strBonusC = "," + winBonus * 2
                        strBonusA = "," + winBonus * -1
                        strBonusB = "," + winBonus * -1
                        resultC += winBonus * 2
                        resultA -= winBonus
                        resultB -= winBonus
                    }
                }

            }
            var pt = context.getString(R.string.player)
            if (!isSingleGame)
                pt = context.getString(R.string.team, 0)
            pt = pt.replace("0", "")
            pt = pt.replace(" ", "")
            accounting = "\n" + context.getString(R.string.accounting) + ":\n"
            accounting = accounting + pt + " A: " + getDTS(resultA) + " " + monetaryUnit
            accounting = accounting + " (" + getDTS(resultA0) + "," + getDTS(resultA1) + "," + getDTS(resultA2) + strBonusA + ")\n"
            accounting = accounting + pt + " B: " + getDTS(resultB) + " " + monetaryUnit
            accounting = accounting + " (" + getDTS(resultB0) + "," + getDTS(resultB1) + "," + getDTS(resultB2) + strBonusB + ")\n"
            if (playerNumber == 3) {
                accounting = accounting + pt + " C: " + getDTS(resultC) + " " + monetaryUnit
                accounting = accounting + " (" + getDTS(resultC0) + "," + getDTS(resultC1) + "," + getDTS(resultC2) + strBonusC + ")"
            }

            return accounting
        }
    private var playerResultA = ""
    private var playerResultB = ""

    val playerResult: String
        get() {
            var sumA0 = 0
            var sumB0 = 0
            var sumA1 = 0
            var sumB1 = 0
            var sumA2 = 0
            var sumB2 = 0

            var sA = 0
            var sB = 0

            for (j in 0 until ROWS) {

                if (resultsPlayerA[0][j] >= 0)
                    sumA0 += resultsPlayerA[0][j]
                if (resultsPlayerB[0][j] >= 0)
                    sumB0 += resultsPlayerB[0][j]

                if (resultsPlayerA[1][j] >= 0)
                    sumA1 += resultsPlayerA[1][j]
                if (resultsPlayerB[1][j] >= 0)
                    sumB1 += resultsPlayerB[1][j]

                if (resultsPlayerA[2][j] >= 0)
                    sumA2 += resultsPlayerA[2][j]
                if (resultsPlayerB[2][j] >= 0)
                    sumB2 += resultsPlayerB[2][j]

            }

            if (sumA0 != sumB0) {
                if (sumA0 > sumB0) {
                    sA += pointsColumn1
                } else {
                    sB += pointsColumn1
                }
            }

            if (sumA1 != sumB1) {
                if (sumA1 > sumB1) {
                    sA += pointsColumn2
                } else {
                    sB += pointsColumn2
                }
            }

            if (sumA2 != sumB2) {
                if (sumA2 > sumB2) {
                    sA += pointsColumn3
                } else {
                    sB += pointsColumn3
                }
            }

            if (sumA0 > sumB0 && sumA1 > sumB1 && sumA2 > sumB2)
                sA += pointsBonus

            if (sumB0 > sumA0 && sumB1 > sumA1 && sumB2 > sumA2)
                sB += pointsBonus

            if (sA == 0 && sB == 0) {
                playerResultA = "0"
                playerResultB = "0"
            } else {
                val iAbs = abs(sA - sB)
                if (sA > sB) {
                    playerResultA = " +$iAbs"
                    playerResultB = " -$iAbs"
                } else {
                    playerResultA = " -$iAbs"
                    playerResultB = " +$iAbs"
                }
            }

            return "$sA - $sB"

        }

    private val restMoves: String
        get() {
            var moves: String
            var cntA = 0
            var cntB = 0
            var cntC = 0
            for (i in 0 until COLS) {
                for (j in 0 until ROWS) {
                    if (resultsPlayerA[i][j] < 0)
                        cntA++
                    if (resultsPlayerB[i][j] < 0)
                        cntB++
                    if (resultsPlayerC[i][j] < 0)
                        cntC++
                }
            }
            moves =
                if (cntA > cntB)
                    "" + cntA
                else
                    "" + cntB
            if (playerNumber == 3) {
                if ((cntC > cntA) and (cntC > cntB))
                    moves = "" + cntC
            }
            return moves
        }

    val colValues: String
        get() {
            val m = payoutMultiplier
            return "" + pointsColumn1 * m + " " + pointsColumn2 * m + " " + pointsColumn3 * m + " - " + pointsBonus * m
        }

    internal var dice: Int = 0               // 0: manual, 1: automatic
    var playerNumber: Int = 0       // min: 2, max: 3
    var isSingleGame = true    // single: true, double(team): false
    var isPlayerColumn = false
    var isSummation = false
    var isFlipScreen = false
    var isEntry = false
    private lateinit var playerNameA: String
    private lateinit var playerNameB: String
    private lateinit var playerNameC: String
    var playerStart = 'A'
    var playerToMove = 'A'
    var nextPlayerToMove = 'B'
    var prevPlayerToMove = 'A'
    var pointsColumn1 = 1
    var pointsColumn2 = 2
    var pointsColumn3 = 4
    var pointsBonus = 3
    var payoutMultiplier: Int = 0
    var monetaryUnit: String? = null
    internal lateinit var resultsPlayerA: Array<IntArray>
    internal lateinit var resultsPlayerB: Array<IntArray>
    internal lateinit var resultsPlayerC: Array<IntArray>
    var bonusServed = 0
    var bonusServedGrande = 0

    private var displayWidth = 480
    private var displayHeight = 800
    private var displayDensity = 1.0f
    private var textSize = 24
    private var gridItemWidth = 48
    private var gridItemHeight = 48
    var diceImageSize = 48

    var onlineCheckId = ""

    private var diceResult = ""
    private var diceResultDouble1 = ""
    var diceText = ""
    var diceTextDouble1 = ""
    var icon1: Int = 0
    var icon2: Int = 0
    var icon3: Int = 0
    var icon4: Int = 0
    var icon5: Int = 0
    var icon6: Int = 0
    var diceIcons = IntArray(6)
    private var t0 = "1"
    private var t1 = "2"
    private var t2 = "3"
    private var t3 = "4"
    private var t4 = "5"
    private var t5 = "6"
    private var t6 = "S"
    private var t7 = "F"
    private var t8 = "P"
    private var t9 = "G"
    private var n0 = "1"
    private var n1 = "2"
    private var n2 = "3"
    private var n3 = "4"
    private var n4 = "5"
    private var n5 = "6"
    private var n6 = "20"
    private var n7 = "30"
    private var n8 = "40"
    private var n9 = "50"

    var selectedGridItem = -1
    internal lateinit var gridCurrent: Array<String>

    init {
        this.displayWidth = displayWidth
        this.displayHeight = displayHeight
        this.displayDensity = displayDensity
        gridItemHeight =
            if (displayOrientation == Configuration.ORIENTATION_PORTRAIT)
                this.displayHeight * 58 / 100 / 12
            else
                this.displayHeight / 13
        textSize = gridItemHeight * 75 / 100
        diceImageSize = (this.displayHeight - this.displayHeight * 65 / 100) / 4
        setDiceIconValues(prefs)
        initData(runPrefs, prefs)

//        if (diceImageSize < 0) {
//            Log.i(TAG, "w: $displayWidth, h: $displayHeight,dens: $displayDensity")
//            Log.i(TAG, "gridH: $gridItemHeight, imageH: $diceImageSize")
//        }

    }

    fun setDiceIconValues(prefs: SharedPreferences) {
        if (prefs.getInt("icons", 1) == 1) {
            icon1 = R.drawable._1_1
            icon2 = R.drawable._1_2
            icon3 = R.drawable._1_3
            icon4 = R.drawable._1_4
            icon5 = R.drawable._1_5
            icon6 = R.drawable._1_6
            for (i in diceIcons.indices) {
                when (i) {
                    0 -> diceIcons[i] = R.drawable._1_1
                    1 -> diceIcons[i] = R.drawable._1_2
                    2 -> diceIcons[i] = R.drawable._1_3
                    3 -> diceIcons[i] = R.drawable._1_4
                    4 -> diceIcons[i] = R.drawable._1_5
                    5 -> diceIcons[i] = R.drawable._1_6
                }
            }
            t0 = "1"
            t1 = "2"
            t2 = "3"
            t3 = "4"
            t4 = "5"
            t5 = "6"
            t6 = context.resources.getString(R.string.val_s)
            t7 = context.resources.getString(R.string.val_f)
            t8 = context.resources.getString(R.string.val_p)
            t9 = context.resources.getString(R.string.val_g)
        }
        if (prefs.getInt("icons", 1) == 2) {
            icon1 = R.drawable._2_1
            icon2 = R.drawable._2_2
            icon3 = R.drawable._2_3
            icon4 = R.drawable._2_4
            icon5 = R.drawable._2_5
            icon6 = R.drawable._2_6
            for (i in diceIcons.indices) {
                when (i) {
                    0 -> diceIcons[i] = R.drawable._2_1
                    1 -> diceIcons[i] = R.drawable._2_2
                    2 -> diceIcons[i] = R.drawable._2_3
                    3 -> diceIcons[i] = R.drawable._2_4
                    4 -> diceIcons[i] = R.drawable._2_5
                    5 -> diceIcons[i] = R.drawable._2_6
                }
            }
            t0 = context.resources.getString(R.string.val_9)
            t1 = context.resources.getString(R.string.val_10)
            t2 = context.resources.getString(R.string.val_j)
            t3 = context.resources.getString(R.string.val_q)
            t4 = context.resources.getString(R.string.val_k)
            t5 = context.resources.getString(R.string.val_a)
            t6 = context.resources.getString(R.string.val_s)
            t7 = context.resources.getString(R.string.val_f)
            t8 = context.resources.getString(R.string.val_p)
            t9 = context.resources.getString(R.string.val_g)
        }
        if (prefs.getInt("icons", 1) == 3) {
            icon1 = R.drawable._3_1
            icon2 = R.drawable._3_2
            icon3 = R.drawable._3_3
            icon4 = R.drawable._3_4
            icon5 = R.drawable._3_5
            icon6 = R.drawable._3_6
            for (i in diceIcons.indices) {
                when (i) {
                    0 -> diceIcons[i] = R.drawable._3_1
                    1 -> diceIcons[i] = R.drawable._3_2
                    2 -> diceIcons[i] = R.drawable._3_3
                    3 -> diceIcons[i] = R.drawable._3_4
                    4 -> diceIcons[i] = R.drawable._3_5
                    5 -> diceIcons[i] = R.drawable._3_6
                }
            }
            t0 = "1"
            t1 = "2"
            t2 = "3"
            t3 = "4"
            t4 = "5"
            t5 = "6"
            t6 = context.resources.getString(R.string.val_s)
            t7 = context.resources.getString(R.string.val_f)
            t8 = context.resources.getString(R.string.val_p)
            t9 = context.resources.getString(R.string.val_g)
        }
        n0 = "1"
        n1 = "2"
        n2 = "3"
        n3 = "4"
        n4 = "5"
        n5 = "6"
        n6 = "20"
        n7 = "30"
        n8 = "40"
        n9 = "50"
    }

    private fun initData(runPrefs: SharedPreferences, prefs: SharedPreferences) {
        dice = prefs.getInt("dice", 1)
        playerNumber = prefs.getInt("players", 2)
        selectedGridItem = runPrefs.getInt("selectedGridItem", -1)
        if (runPrefs.getString("playerStart", "B")!!.length == 1)
            playerStart = runPrefs.getString("playerStart", "B")!![0]
        if (runPrefs.getString("playerToMove", "B")!!.length == 1)
            playerToMove = runPrefs.getString("playerToMove", "B")!![0]
        if ((playerNumber == 2) and (playerStart == 'C')) {
            playerStart = 'A'
            playerToMove = playerStart
        }
        computeNextPlayerToMove(playerToMove)
        computePrevPlayerToMove(playerToMove)
        isSingleGame = prefs.getBoolean("isSingleGame", true)
        isPlayerColumn = prefs.getBoolean("isPlayerColumn", true)
        isSummation = prefs.getBoolean("isSummation", false)
        isFlipScreen = prefs.getBoolean("computeFlipScreen", false)

        if (playerNumber == 3) {
            isSummation = true
            isFlipScreen = false
        }

        playerNameA = "A"
        playerNameB = "B"
        playerNameC = "C"
        pointsColumn1 = prefs.getInt("pointsCol1", 1)
        pointsColumn2 = prefs.getInt("pointsCol2", 2)
        pointsColumn3 = prefs.getInt("pointsCol3", 4)
        pointsBonus = prefs.getInt("pointsBon", 2)
        payoutMultiplier = prefs.getInt("multiplier", 1)
        monetaryUnit = prefs.getString("unit", context.resources.getString(R.string.points))
        resultsPlayerA = Array(COLS) { IntArray(ROWS) }
        resultsPlayerB = Array(COLS) { IntArray(ROWS) }
        resultsPlayerC = Array(COLS) { IntArray(ROWS) }
        bonusServed = prefs.getInt("bonusServed", 5)
        bonusServedGrande = prefs.getInt("bonusServedGrande", 30)

        var st: StringTokenizer
        // values playerName A
        var stX = runPrefs.getString("A0", PREFS_DEFAULT)
        st = StringTokenizer(stX, ",")
        for (i in 0 until ROWS) {
            resultsPlayerA[0][i] = Integer.parseInt(st.nextToken())
        }
        stX = runPrefs.getString("A1", PREFS_DEFAULT)
        st = StringTokenizer(stX, ",")
        for (i in 0 until ROWS) {
            resultsPlayerA[1][i] = Integer.parseInt(st.nextToken())
        }
        stX = runPrefs.getString("A2", PREFS_DEFAULT)
        st = StringTokenizer(stX, ",")
        for (i in 0 until ROWS) {
            resultsPlayerA[2][i] = Integer.parseInt(st.nextToken())
        }
        // values playerName B
        stX = runPrefs.getString("B0", PREFS_DEFAULT)
        st = StringTokenizer(stX, ",")
        for (i in 0 until ROWS) {
            resultsPlayerB[0][i] = Integer.parseInt(st.nextToken())
        }
        stX = runPrefs.getString("B1", PREFS_DEFAULT)
        st = StringTokenizer(stX, ",")
        for (i in 0 until ROWS) {
            resultsPlayerB[1][i] = Integer.parseInt(st.nextToken())
        }
        stX = runPrefs.getString("B2", PREFS_DEFAULT)
        st = StringTokenizer(stX, ",")
        for (i in 0 until ROWS) {
            resultsPlayerB[2][i] = Integer.parseInt(st.nextToken())
        }
        // values playerName C
        stX = runPrefs.getString("C0", PREFS_DEFAULT)
        st = StringTokenizer(stX, ",")
        for (i in 0 until ROWS) {
            resultsPlayerC[0][i] = Integer.parseInt(st.nextToken())
        }
        stX = runPrefs.getString("C1", PREFS_DEFAULT)
        st = StringTokenizer(stX, ",")
        for (i in 0 until ROWS) {
            resultsPlayerC[1][i] = Integer.parseInt(st.nextToken())
        }
        stX = runPrefs.getString("C2", PREFS_DEFAULT)
        //karl
        if (stX != null) {
            if (stX.contains("\"]"))
                stX = stX.replace("\"]", "")
            st = StringTokenizer(stX, ",")
            for (i in 0 until ROWS) {
                resultsPlayerC[2][i] = Integer.parseInt(st.nextToken())
            }
        }

        setGridControl()
    }

    fun setGridControl() {
        if (playerNumber == 2) {
            gridCurrent =
                if (!isPlayerColumn)
                    GRID_COL_PLAYER2
                else
                    GRID_PLAY_PLAYER2
            gridItemWidth = displayWidth / 8
        }
        if (playerNumber == 3) {
            gridCurrent =
                if (!isPlayerColumn)
                    GRID_COL_PLAYER3
                else
                    GRID_PLAY_PLAYER3
            gridItemWidth = displayWidth / 11
        }
    }

    fun setSelectedGridItem(selectedId: String) {
        selectedGridItem = -1
        for (i in gridCurrent.indices) {
            if (gridCurrent[i] == selectedId) {
                selectedGridItem = i
                break
            }
        }
    }

    fun getPlayerPrefs(playerId: Char, col: Int): String {
        var pref = ""
        when (playerId) {
            'A' -> for (i in 0 until ROWS) {
                pref =
                    if (i < ROWS - 1)
                        pref + resultsPlayerA[col][i] + ","
                    else
                        pref + resultsPlayerA[col][i]
            }
            'B' -> for (i in 0 until ROWS) {
                pref =
                    if (i < ROWS - 1)
                        pref + resultsPlayerB[col][i] + ","
                    else
                        pref + resultsPlayerB[col][i]
            }
            'C' -> for (i in 0 until ROWS) {
                pref =
                    if (i < ROWS - 1)
                        pref + resultsPlayerC[col][i] + ","
                    else
                        pref + resultsPlayerC[col][i]
            }
        }
        return pref
    }

    fun getPlayerEngineValues(playerId: Char, col: Int): String {
        var pref = ""
        var v: String
        val command = "setcol"
        when (playerId) {
            'A' -> {
                pref = "$command $playerId $col "
                for (i in 0 until ROWS) {
                    v = "" + resultsPlayerA[col][i]
                    if (resultsPlayerA[col][i] == -1) v = "-"
                    pref =
                        if (i < ROWS - 1)
                            "$pref$v,"
                        else
                            pref + v + ""
                }
            }
            'B' -> {
                pref = "$command $playerId $col "
                for (i in 0 until ROWS) {
                    v = "" + resultsPlayerB[col][i]
                    if (resultsPlayerB[col][i] == -1) v = "-"
                    pref =
                        if (i < ROWS - 1)
                            "$pref$v,"
                        else
                            pref + v + ""
                }
            }
            'C' -> {
                pref = "$command $playerId $col "
                for (i in 0 until ROWS) {
                    v = "" + resultsPlayerC[col][i]
                    if (resultsPlayerC[col][i] == -1) v = "-"
                    pref =
                        if (i < ROWS - 1)
                            "$pref$v,"
                        else
                            pref + v + ""
                }
            }
        }
        return pref
    }

    fun setPlayerResult(playerId: Char, col: Int, row: Int, value: Int): Boolean {
        var isUpdated = false
        isEntry = false
        when (playerId) {
            'A' -> if (resultsPlayerA[col][row] < 0) {
                if ((playerToMove == 'A') or (playerToMove == ' ')) {
                    resultsPlayerA[col][row] = value
                    isUpdated = true
                }
            } else
                isEntry = true
            'B' -> if (resultsPlayerB[col][row] < 0) {
                if ((playerToMove == 'B') or (playerToMove == ' ')) {
                    resultsPlayerB[col][row] = value
                    isUpdated = true
                }
            } else
                isEntry = true
            'C' -> if (resultsPlayerC[col][row] < 0) {
                if ((playerToMove == 'C') or (playerToMove == ' ')) {
                    resultsPlayerC[col][row] = value
                    isUpdated = true
                }
            } else
                isEntry = true
        }
        return isUpdated
    }

    fun getPlayerResult(playerId: Char, col: Int, row: Int): Int {
        return when (playerId) {
            'A' -> resultsPlayerA[col][row]
            'B' -> resultsPlayerB[col][row]
            'C' -> resultsPlayerC[col][row]
            else -> -2
        }
    }

    fun getNextPlayerAB(starter: Char): Char {
        var cntA = 0
        var cntB = 0
        for (j in 0 until ROWS) {
            if (resultsPlayerA[0][j] >= 0)
                cntA++
            if (resultsPlayerB[0][j] >= 0)
                cntB++
            if (resultsPlayerA[1][j] >= 0)
                cntA++
            if (resultsPlayerB[1][j] >= 0)
                cntB++
            if (resultsPlayerA[2][j] >= 0)
                cntA++
            if (resultsPlayerB[2][j] >= 0)
                cntB++
        }
        if (cntA > cntB)
            return 'B'
        if (cntB > cntA)
            return 'A'
        return starter
    }

    fun cancelPlayerResult(position: Int) {
        val gridId = gridCurrent[position]
        var playerId = ' '
        var col = 0
        var row = 0
        if (gridId.startsWith("A") or gridId.startsWith("B") or (gridId.startsWith("C") and (gridId.length == 3))) {
            playerId = gridId[0]
            col = Character.getNumericValue(gridId[1])
            row = Character.getNumericValue(gridId[2])
        }
        //        Log.i(TAG, "position: " + position + ", playerId: " + playerId);
        if (playerId != ' ') {
            when (playerId) {
                'A' -> {
                    resultsPlayerA[col][row] = -1
                    playerToMove = 'A'
                }
                'B' -> {
                    resultsPlayerB[col][row] = -1
                    playerToMove = 'B'
                }
                'C' -> {
                    resultsPlayerC[col][row] = -1
                    playerToMove = 'C'
                }
            }
            computeNextPlayerToMove(playerToMove)
        }
    }

    fun getGridItemPosition(player: Char, col: Int, row: Int): Int {
        var pos = 0
        val itemId = "" + player + col + row
        for (i in gridCurrent.indices) {
            if (gridCurrent[i] == itemId) {
                pos = i
                break
            }
        }
        return pos
    }

    fun getResultFromSelectedGridItem(row: Int, count: Int): Int {
        var result: Int
        if (row <= 5)
            result = PICTURE_MULTIPLIER[row] * count
        else {
            result = PICTURE_MULTIPLIER[row]
            if (count == 0)
                result = 0
            if (count == 2)
            {
                result +=
                    if (row == 9)   // served
                        bonusServedGrande
                    else
                        bonusServed
            }
        }
        return result
    }

    fun getResultFromDiceValues(diceValue: Int, isServed: Boolean, diceValues: IntArray): Int {
        //        Log.i(TAG, "diceValue: " + diceValue + ", isServed: " + isServed);
        //        Log.i(TAG, "imageCount[]: " + imageCount[0] + ", "  + imageCount[1] + ", " + imageCount[2]
        //                + ", " + imageCount[3] + ", " + imageCount[4] + ", " + imageCount[5] + ", ");
        var result = 0
        var isStraight = false
        var isFull = false
        var isGrande = false
        var isPoker = false
        var is3 = false
        var is2 = false

        if ((diceValues[0] == 1) and (diceValues[1] == 1) and (diceValues[2] == 1) and (diceValues[3] == 1) and (diceValues[4] == 1))
            isStraight = true
        if ((diceValues[1] == 1) and (diceValues[2] == 1) and (diceValues[3] == 1) and (diceValues[4] == 1) and (diceValues[5] == 1))
            isStraight = true
        for (diceValue1 in diceValues) {
            if (diceValue1 == 5)
                isGrande = true
            if (diceValue1 == 4)
                isPoker = true
            if (diceValue1 == 3)
                is3 = true
            if (diceValue1 == 2)
                is2 = true
        }
        if (is3 and is2)
            isFull = true

        when (diceValue) {
            0, 1, 2, 3, 4, 5 -> result = PICTURE_MULTIPLIER[diceValue] * diceValues[diceValue]
            6 -> if (isStraight) {
                result = PICTURE_MULTIPLIER[diceValue]
                if (isServed)
                    result += bonusServed
            }
            7 -> if (isFull) {
                result = PICTURE_MULTIPLIER[diceValue]
                if (isServed)
                    result += bonusServed
            }
            8 -> if (isPoker) {
                result = PICTURE_MULTIPLIER[diceValue]
                if (isServed)
                    result += bonusServed
            }
            9 -> if (isGrande) {
                result = PICTURE_MULTIPLIER[diceValue]
                if (isServed)
                    result += bonusServedGrande
            }
        }

        return result
    }

    fun getDiceText(isDouble1: Boolean, isServed: Boolean, diceValues: IntArray, diceModus: Int): String {
        //Log.i(TAG, "getDiceText(), isDouble1: " + isDouble1 + ", diceValues: " + diceValues[0]+ diceValues[1]+ diceValues[2]+ diceValues[3]+ diceValues[4]+ diceValues[5]);
        var isStraight = false
        var isFull = false
        var isGrande = false
        var isPoker = false
        var is3 = false
        var is2 = false
        var cnt2 = 0

        if ((diceValues[0] == 1) and (diceValues[1] == 1) and (diceValues[2] == 1) and (diceValues[3] == 1) and (diceValues[4] == 1))
            isStraight = true
        if ((diceValues[1] == 1) and (diceValues[2] == 1) and (diceValues[3] == 1) and (diceValues[4] == 1) and (diceValues[5] == 1))
            isStraight = true
        for (diceValue in diceValues) {
            if (diceValue == 5)
                isGrande = true
            if (diceValue == 4)
                isPoker = true
            if (diceValue == 3)
                is3 = true
            if (diceValue == 2) {
                is2 = true
                cnt2++
            }
        }
        if (is3 and is2)
            isFull = true

        diceText = context.resources.getString(R.string.diceRunt)
        if (cnt2 == 1)
            diceText = context.resources.getString(R.string.dicePair)
        if (cnt2 == 2)
            diceText = context.resources.getString(R.string.diceTwoPair)
        if (is3)
            diceText = context.resources.getString(R.string.diceThreeOfKind)
        if (isStraight)
            diceText = context.resources.getString(R.string.diceStraight)
        if (isFull)
            diceText = context.resources.getString(R.string.diceFullHouse)
        if (isPoker)
            diceText = context.resources.getString(R.string.dicePoker)
        if (isGrande)
            diceText = context.resources.getString(R.string.diceGrande)
        if (isServed) {
            if (isStraight or isFull or isPoker or isGrande)
                diceText = diceText + " " + context.resources.getString(R.string.diceServed)
        }
        diceResult = diceText
        if (!isSingleGame and isDouble1)
            diceResultDouble1 = diceText
        if (isNewGame and (diceModus == 0)) {
            if (isSingleGame or (!isSingleGame and isDouble1))
                diceText = context.resources.getString(R.string.newGame)
        }
        if (isGameOver)
            diceText = context.resources.getString(R.string.gameOver)

        return diceText

    }

    fun getDiceResult(isServed: Boolean, diceValues: IntArray): String {
        var isStraight = false
        var isFull = false
        var isGrande = false
        var isPoker = false
        var is3 = false
        var is2 = false
        var cnt2 = 0

        if ((diceValues[0] == 1) and (diceValues[1] == 1) and (diceValues[2] == 1) and (diceValues[3] == 1) and (diceValues[4] == 1))
            isStraight = true
        if ((diceValues[1] == 1) and (diceValues[2] == 1) and (diceValues[3] == 1) and (diceValues[4] == 1) and (diceValues[5] == 1))
            isStraight = true
        var cnt0 = 0
        for (diceValue in diceValues) {
            if (diceValue == 5)
                isGrande = true
            if (diceValue == 4)
                isPoker = true
            if (diceValue == 3)
                is3 = true
            if (diceValue == 2) {
                is2 = true
                cnt2++
            }
            if (diceValue == 0)
                cnt0++
        }
        if (is3 and is2)
            isFull = true

        var diceResult = context.resources.getString(R.string.diceRunt)
        if (cnt2 == 1)
            diceResult = context.resources.getString(R.string.dicePair)
        if (cnt2 == 2)
            diceResult = context.resources.getString(R.string.diceTwoPair)
        if (is3)
            diceResult = context.resources.getString(R.string.diceThreeOfKind)
        if (isStraight)
            diceResult = context.resources.getString(R.string.diceStraight)
        if (isFull)
            diceResult = context.resources.getString(R.string.diceFullHouse)
        if (isPoker)
            diceResult = context.resources.getString(R.string.dicePoker)
        if (isGrande)
            diceResult = context.resources.getString(R.string.diceGrande)
        if (isServed) {
            if (isStraight or isFull or isPoker or isGrande)
                diceResult = diceResult + " " + context.resources.getString(R.string.diceServed)
        }
        if (cnt0 == 6)
            diceResult = ""
        return diceResult
    }

    fun setStartPlayer(gridId: String) {
        var cntA = 0
        var cntB = 0
        var cntC = 0
        for (i in 0 until COLS) {
            for (j in 0 until ROWS) {
                if (resultsPlayerA[i][j] < 0)
                    cntA++
                if (resultsPlayerB[i][j] < 0)
                    cntB++
                if (resultsPlayerC[i][j] < 0)
                    cntC++
            }
        }
        if (gridId.startsWith("L") and (gridId.length == 3)) {
            if ((cntA == 30) and (cntB == 30) and (cntC == 30)) {
                playerStart = gridId[1]
                playerToMove = gridId[1]
                computeNextPlayerToMove(playerToMove)
            }
        }
    }

    private fun getPlayerValueFromArray(arrayId: String): String {
        var playerValue = ""
        var col: Int
        val row: Int
        if (arrayId.startsWith("S") and (arrayId.length == 3) and Character.isDigit(arrayId[2])) {
            col = Character.getNumericValue(arrayId[2])
            var sumA = 0
            var sumB = 0
            var sumC = 0
            var strA = "-"
            var strB = "-"
            var strC = "-"
//            for (j in 0 until resultsPlayerA[col].size) {
            val y = resultsPlayerA[col].size -1
            for (j in 0 .. y) {
                if (resultsPlayerA[col][j] >= 0)
                    sumA += resultsPlayerA[col][j]
                if (resultsPlayerB[col][j] >= 0)
                    sumB += resultsPlayerB[col][j]
                if (resultsPlayerC[col][j] >= 0)
                    sumC += resultsPlayerC[col][j]
            }
            if (playerNumber == 2) {
                if (sumA == sumB) {
                    strA = "0"
                    strB = "0"
                } else {
                    if (sumA > sumB)
                        strA = "" + (sumA - sumB)
                    else
                        strB = "" + (sumB - sumA)
                }
            }
            if (playerNumber == 3) {
                if ((sumA == sumB) and (sumA == sumC)) {
                    strA = "0"
                    strB = "0"
                    strC = "0"
                } else {
                    if ((sumA > sumB) and (sumA > sumC)) {
                        strA = "" + (sumA - sumB)
                        if (sumC > sumB)
                            strA = "" + (sumA - sumC)
                    }
                    if ((sumB > sumA) and (sumB > sumC)) {
                        strB = "" + (sumB - sumA)
                        if (sumC > sumA)
                            strB = "" + (sumB - sumC)
                    }
                    if ((sumC > sumA) and (sumC > sumB)) {
                        strC = "" + (sumC - sumA)
                        if (sumB > sumA)
                            strC = "" + (sumC - sumB)
                    }
                }
            }
            when (arrayId[1]) {
                'A' ->  playerValue =
                            if (isSummation)
                                "" + sumA
                            else
                                "" + strA
                'B' ->  playerValue =
                            if (isSummation)
                                "" + sumB
                            else
                                "" + strB
                'C' ->  playerValue =
                            if (isSummation)
                                "" + sumC
                            else
                                "" + strC
            }
        }
        if (arrayId.startsWith("A") or arrayId.startsWith("B") or arrayId.startsWith("C")) {
            if (arrayId.length == 3) {
                if (Character.isDigit(arrayId[1]) and Character.isDigit(arrayId[2])) {
                    col = Character.getNumericValue(arrayId[1])
                    row = Character.getNumericValue(arrayId[2])
                    when (arrayId[0]) {
                        'A' ->  playerValue =
                                    if (resultsPlayerA[col][row] >= 0)
                                        "" + resultsPlayerA[col][row]
                                    else
                                        "-"
                        'B' ->  playerValue =
                                    if (resultsPlayerB[col][row] >= 0)
                                        "" + resultsPlayerB[col][row]
                                    else
                                        "-"
                        'C' ->  playerValue =
                                    if (resultsPlayerC[col][row] >= 0)
                                        "" + resultsPlayerC[col][row]
                                    else
                                        "-"
                    }
                }
            }
        }

        return playerValue
    }

    private fun getDTS(dbl: Double): String {
        return if (dbl.toString().endsWith(".0"))
            dbl.toInt().toString()
        else
            dbl.toString()
    }

    fun setPlayerToMove(engineName: String): String {
        var msg: String
        var cntA = 0
        var cntB = 0
        var cntC = 0
        for (i in 0 until COLS) {
            for (j in 0 until ROWS) {
                if (resultsPlayerA[i][j] < 0)
                    cntA++
                if (resultsPlayerB[i][j] < 0)
                    cntB++
                if (resultsPlayerC[i][j] < 0)
                    cntC++
            }
        }
        if ((playerNumber == 2) and (cntA == 30) and (cntB == 30) or ((playerNumber == 3) and (cntA == 30) and (cntB == 30) and (cntC == 30))) {
            msg = context.resources.getString(R.string.newGame) + ": " + playerToMove
        } else {
            if (playerNumber == 2) {
                playerToMove = when {
                    cntA == cntB    -> playerStart
                    cntA >  cntB    -> 'A'
                    else            -> 'B'
                }
            }
            if (playerNumber == 3) {
                if ((cntA == cntB) and (cntA == cntC))
                    playerToMove = playerStart
                else {
                    if (cntA > cntC)
                        playerToMove = 'A'
                    else {
                        if (cntB > cntA)
                            playerToMove = 'B'
                        else if (cntC > cntB)
                            playerToMove = 'C'
                    }
                }
            }
            msg = context.resources.getString(R.string.player) + " " + playerToMove
            if (!isSingleGame)
                msg = context.resources.getString(R.string.team, 0) + " " + playerToMove
            msg = msg.replace("0", "")
            msg = msg.replace(" ", "")
            if (engineName != "")
                msg = engineName
            if (!isSingleGame) {
                if ((dice == 1) and !isSingleGame)
                    msg = "$msg(1)"
            }
        }
        if (dice == 1)
            msg = msg + ", 1. " + context.resources.getString(R.string.diceTry)

        computeNextPlayerToMove(playerToMove)
        computePrevPlayerToMove(playerToMove)
        return msg
    }

    fun computeNextPlayerToMove(playerToMove: Char) {
        when (playerToMove) {
            'A' -> nextPlayerToMove = 'B'
            'B' ->  nextPlayerToMove =
                        if (playerNumber == 3)
                            'C'
                        else
                            'A'
            'C' -> nextPlayerToMove = 'A'
        }
    }

    fun computePrevPlayerToMove(playerToMove: Char) {
        when (playerToMove) {
            'A' ->  prevPlayerToMove =
                        if (playerNumber == 3)
                            'C'
                        else
                            'B'
            'B' -> prevPlayerToMove = 'A'
            'C' -> prevPlayerToMove = 'B'
        }
    }

    fun getPrevPlayer(playerToMove: Char): Char {
        return when (playerToMove) {
            'A' ->  if (playerNumber == 3)
                        'C'
                    else
                        'B'
            'B' -> 'A'
            'C' -> 'B'
            else -> 'A'
        }
    }

    companion object {
        const val TAG = "EscaleroData"
        const val PREFS_DEFAULT = "-1,-1,-1,-1,-1,-1,-1,-1,-1,-1"

        const val COLS = 3
        const val ROWS = 10
        const val LA0 = "A1"
        const val LB0 = "B1"
        const val LC0 = "C1"
        const val LA1 = "A2"
        const val LB1 = "B2"
        const val LC1 = "C2"
        const val LA2 = "A3"
        const val LB2 = "B3"
        const val LC2 = "C3"

        const val X01N = "\u22a1"
        const val X01F = "\u21f3"
        const val X02S = "+"
        const val X02D = "+-"
        const val X03 = "#"

        val PICTURE_MULTIPLIER = intArrayOf(1, 2, 3, 4, 5, 6, 20, 30, 40, 50)

        val GRID_COL_PLAYER2 = arrayOf(
            "X00", "LA0", "LB0", "LA1", "LB1", "LA2", "LB2", "X01",
            "T0",  "A00", "B00", "A10", "B10", "A20", "B20", "N0",
            "T1",  "A01", "B01", "A11", "B11", "A21", "B21", "N1",
            "T2",  "A02", "B02", "A12", "B12", "A22", "B22", "N2",
            "T3",  "A03", "B03", "A13", "B13", "A23", "B23", "N3",
            "T4",  "A04", "B04", "A14", "B14", "A24", "B24", "N4",
            "T5",  "A05", "B05", "A15", "B15", "A25", "B25", "N5",
            "T6",  "A06", "B06", "A16", "B16", "A26", "B26", "N6",
            "T7",  "A07", "B07", "A17", "B17", "A27", "B27", "N7",
            "T8",  "A08", "B08", "A18", "B18", "A28", "B28", "N8",
            "T9",  "A09", "B09", "A19", "B19", "A29", "B29", "N9",
            "X02", "SA0", "SB0", "SA1", "SB1", "SA2", "SB2", "X03"
        )
        val GRID_PLAY_PLAYER2 = arrayOf(
            "X00", "LA0", "LA1", "LA2", "LB0", "LB1", "LB2", "X01",
            "T0",  "A00", "A10", "A20", "B00", "B10", "B20", "N0",
            "T1",  "A01", "A11", "A21", "B01", "B11", "B21", "N1",
            "T2",  "A02", "A12", "A22", "B02", "B12", "B22", "N2",
            "T3",  "A03", "A13", "A23", "B03", "B13", "B23", "N3",
            "T4",  "A04", "A14", "A24", "B04", "B14", "B24", "N4",
            "T5",  "A05", "A15", "A25", "B05", "B15", "B25", "N5",
            "T6",  "A06", "A16", "A26", "B06", "B16", "B26", "N6",
            "T7",  "A07", "A17", "A27", "B07", "B17", "B27", "N7",
            "T8",  "A08", "A18", "A28", "B08", "B18", "B28", "N8",
            "T9",  "A09", "A19", "A29", "B09", "B19", "B29", "N9",
            "X02", "SA0", "SA1", "SA2", "SB0", "SB1", "SB2", "X03"
        )
        val GRID_COL_PLAYER3 = arrayOf(
            "X00", "LA0", "LB0", "LC0", "LA1", "LB1", "LC1", "LA2", "LB2", "LC2", "X01",
            "T0",  "A00", "B00", "C00", "A10", "B10", "C10", "A20", "B20", "C20", "N0",
            "T1",  "A01", "B01", "C01", "A11", "B11", "C11", "A21", "B21", "C21", "N1",
            "T2",  "A02", "B02", "C02", "A12", "B12", "C12", "A22", "B22", "C22", "N2",
            "T3",  "A03", "B03", "C03", "A13", "B13", "C13", "A23", "B23", "C23", "N3",
            "T4",  "A04", "B04", "C04", "A14", "B14", "C14", "A24", "B24", "C24", "N4",
            "T5",  "A05", "B05", "C05", "A15", "B15", "C15", "A25", "B25", "C25", "N5",
            "T6",  "A06", "B06", "C06", "A16", "B16", "C16", "A26", "B26", "C26", "N6",
            "T7",  "A07", "B07", "C07", "A17", "B17", "C17", "A27", "B27", "C27", "N7",
            "T8",  "A08", "B08", "C08", "A18", "B18", "C18", "A28", "B28", "C28", "N8",
            "T9",  "A09", "B09", "C09", "A19", "B19", "C19", "A29", "B29", "C29", "N9",
            "X02", "SA0", "SB0", "SC0", "SA1", "SB1", "SC1", "SA2", "SB2", "SC2", "X03"
        )
        val GRID_PLAY_PLAYER3 = arrayOf(
            "X00", "LA0", "LA1", "LA2", "LB0", "LB1", "LB2", "LC0", "LC1", "LC2", "X01",
            "T0",  "A00", "A10", "A20", "B00", "B10", "B20", "C00", "C10", "C20", "N0",
            "T1",  "A01", "A11", "A21", "B01", "B11", "B21", "C01", "C11", "C21", "N1",
            "T2",  "A02", "A12", "A22", "B02", "B12", "B22", "C02", "C12", "C22", "N2",
            "T3",  "A03", "A13", "A23", "B03", "B13", "B23", "C03", "C13", "C23", "N3",
            "T4",  "A04", "A14", "A24", "B04", "B14", "B24", "C04", "C14", "C24", "N4",
            "T5",  "A05", "A15", "A25", "B05", "B15", "B25", "C05", "C15", "C25", "N5",
            "T6",  "A06", "A16", "A26", "B06", "B16", "B26", "C06", "C16", "C26", "N6",
            "T7",  "A07", "A17", "A27", "B07", "B17", "B27", "C07", "C17", "C27", "N7",
            "T8",  "A08", "A18", "A28", "B08", "B18", "B28", "C08", "C18", "C28", "N8",
            "T9",  "A09", "A19", "A29", "B09", "B19", "B29", "C09", "C19", "C29", "N9",
            "X02", "SA0", "SA1", "SA2", "SB0", "SB1", "SB2", "SC0", "SC1", "SC2", "X03"
        )
    }
}
