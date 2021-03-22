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

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import java.util.*
import kotlin.math.min
import kotlin.math.sqrt

class BoardView(context: Context, attrs: AttributeSet) : View(context, attrs) {

    private var prefs = context.getSharedPreferences("prefs", 0)
    private var diceSize = 1
    private var isInit = true
    private var diceIcons: IntArray? = null
    var mBoardWidth: Int = 0
    var mBoardHeight: Int = 0
    private var mBoardBorderWidth: Int = 0
    private var mDiceSize = 50
    private var mDiceSizeSmall = 0
    private var mDiceSizeMedium = 0
    private var mDiceSizeLarge = 0
    private var mHoldAreaWidth = 5 * mDiceSize
    private var mHoldAreaHeight = mDiceSize
    private var mRoll = IntArray(5)       // roll image values
    private var mHold = IntArray(5)       // hold image values
    private var mDouble1 = IntArray(5)    // double first playerName image values
    private var mColValues: String? = null              // column values + bonus
    private var mIsSingle: Boolean = false              // single/double
    private var mPlayerToMove: Char = ' '             // 'A' 'B' 'C'
    private var mPlayerInfo: String = ""             // info playerName
    private var mPlayerInfoDouble: String = ""       // info double first playerName
    var mOnlinePlayers: String = ""         // online players
    private var mIsSelectable: Boolean = false          // is roll/hold/double1 selectable ---> VALUES/state
    private var doubleState: Int = 0                // double state: 0=keine Anzeige, 1= Anzeige mit select, 2=nur Anzeige

    private val diceCnt = 5                         // max number of dices
    private val diceValues = 7 // id,state,x,y,diceWidth,diceHeight,rotation (id : res/values/ids, state : 0=unvisible, 1=visible+click, 3=visible)
    private var mHoldIdx = IntArray(diceCnt)                    // [index from mHold]
    private var mRollValues = Array(diceCnt) { IntArray(diceValues) }       // [index from mRoll][VALUES]
    private var mHoldValues = Array(diceCnt) { IntArray(diceValues) }       // [index from mHold][VALUES]
    private var mDouble1Values = Array(diceCnt) { IntArray(diceValues) }    // [index from mDouble1][VALUES]
    private var mControlValues = Array(diceCnt) { IntArray(diceValues) }    // [1=double1Btn, 2=playerInfo, 3=playerInfoDouble][VALUES]
    private val mRand = Random()

    private var mPaint: Paint? = null
    private val mRect: Rect

    init {
        diceSize = prefs.getInt("diceSize", 2)
        mIsSingle = prefs.getBoolean("isSingleGame", true)
        if (diceSize == 3 && !mIsSingle)
            diceSize = 2
        isInit = true
        initValues(true)
        mPaint = Paint()
        mRect = Rect()
    }

    private fun initValues(initRollValues: Boolean) {
        var initValues = initRollValues
        if (!initValues) {
            for (i in 0 until diceCnt) {
                for (j in 0 until diceValues) {
                    if (mRollValues[i][2] <= 0) initValues = true
                    if (mRollValues[i][3] <= 0) initValues = true
                    if (mRollValues[i][4] <= 0) initValues = true
                    if (mRollValues[i][5] <= 0) initValues = true
                }
            }
        }
        for (i in 0 until diceCnt) {
            for (j in 0 until diceValues) {
                if (j == 0)
                // id
                {
                    when (i) {

                        0 -> {
                            mRollValues[i][0] = R.id.dbRoll_1
                            mHoldValues[i][0] = R.id.dbHold_1
                            mDouble1Values[i][0] = R.id.dbDouble_1
                            mControlValues[i][0] = 0
                        }
                        1 -> {
                            mRollValues[i][0] = R.id.dbRoll_2
                            mHoldValues[i][0] = R.id.dbHold_2
                            mDouble1Values[i][0] = R.id.dbDouble_2
                            mControlValues[i][0] = R.id.dbD1
                        }
                        2 -> {
                            mRollValues[i][0] = R.id.dbRoll_3
                            mHoldValues[i][0] = R.id.dbHold_3
                            mDouble1Values[i][0] = R.id.dbDouble_3
                            mControlValues[i][0] = R.id.dbPlayerInfo
                        }
                        3 -> {
                            mRollValues[i][0] = R.id.dbRoll_4
                            mHoldValues[i][0] = R.id.dbHold_4
                            mDouble1Values[i][0] = R.id.dbDouble_4
                            mControlValues[i][0] = R.id.dbPlayerInfoDouble1
                        }
                        4 -> {
                            mRollValues[i][0] = R.id.dbRoll_5
                            mHoldValues[i][0] = R.id.dbHold_5
                            mDouble1Values[i][0] = R.id.dbDouble_5
                            mControlValues[i][0] = 0
                        }
                    }
                } else {
                    if (initValues)
                        mRollValues[i][j] = 0
                    mHoldValues[i][j] = 0
                    mDouble1Values[i][j] = 0
                    mControlValues[i][j] = 0
                }
            }
        }
    }

    fun setDiceIcons(diceIcons: IntArray) {
        this.diceIcons = diceIcons
    }

    fun setDiceSize() {
        diceSize = prefs.getInt("diceSize", 2)
        if (diceSize == 3 && !mIsSingle)
            diceSize = 2
        when (diceSize) {
            1 -> mDiceSize = mDiceSizeSmall
            2 -> mDiceSize = mDiceSizeMedium
            3 -> mDiceSize = mDiceSizeLarge
        }
        mHoldAreaWidth = 5 * mDiceSize
        mHoldAreaHeight = mDiceSize
    }

    fun initBoard() {
        isInit = true
        invalidate()
    }

    fun setRoundValues(colValues: String, isSingle: Boolean, playerToMove: Char, playerInfo: String, playerInfoDouble: String, onlinePlayers: String) {

//        Log.i(TAG, "setRoundValues(), isSingle: $isSingle, playerInfoDouble: $playerInfoDouble")

        mColValues = colValues
        mIsSingle = isSingle
        mPlayerToMove = playerToMove
        mPlayerInfo = playerInfo
        mPlayerInfoDouble = playerInfoDouble
        mOnlinePlayers = onlinePlayers
    }

    fun updateBoard(roll: IntArray, hold: IntArray, double1: IntArray,
                    isSelectable: Boolean, doubleState: Int, initRollValues: Boolean) {

//        Log.i(TAG, "updateBoard(), roll: $roll, hold: $hold, double1: $double1")
//        Log.i(TAG, "updateBoard(), mBoardWidth: $mBoardWidth, mBoardHeight: $mBoardHeight, initRollValues: $initRollValues")
//        Log.i(TAG, "updateBoard(), doubleState: $doubleState, double1.size: ${double1.size}")

        if ((mBoardWidth <= 0) or (mBoardHeight <= 0))
            return

        isInit = false
        mRoll = roll
        mHold = hold

//        Log.i(TAG, "updateBoard(), mRoll: ${mRoll[0]}, ${mRoll[1]}, ${mRoll[2]}, ${mRoll[3]}, ${mRoll[4]}")
//        Log.i(TAG, "updateBoard(), mHold: ${mHold[0]}, ${mHold[1]}, ${mHold[2]}, ${mHold[3]}, ${mHold[4]}")

        mDouble1 = double1
        mIsSelectable = isSelectable
        this.doubleState = doubleState
        initValues(initRollValues)

//        Log.i(TAG, "updateBoard(), mIsSingle: $mIsSingle, doubleState: $doubleState, mPlayerInfoDouble: $mPlayerInfoDouble")

        setControlValues(mIsSingle, doubleState, mPlayerInfo, mControlValues)
        setRollValues(mRoll, mRollValues, isSelectable, initRollValues)
        setHoldValues(mHold, mHoldIdx, mHoldValues, isSelectable)

//        Log.i(TAG, "updateBoard(), mDouble1: ${mDouble1[0]}, ${mDouble1[1]}, ${mDouble1[2]}, ${mDouble1[3]}, ${mDouble1[4]}")
//        Log.i(TAG, "updateBoard(), mDouble1Values: ${mDouble1Values[0]}, ${mDouble1Values[1]}, ${mDouble1Values[2]}, ${mDouble1Values[3]}, ${mDouble1Values[4]}")

        setDoubleValues(mDouble1, mDouble1Values)
        invalidate()
    }

    fun getIdFromTouchPoint(touchPoint: Point): Int {
        for (i in 0 until diceCnt) {
            if (mRollValues[i][1] == 1)
            // state: visible & selectable
            {
                val rollAreaWidth = sqrt((mRollValues[i][4] * mRollValues[i][4] + mRollValues[i][5] * mRollValues[i][5]).toDouble()).toInt()
                val x1 = mRollValues[i][2]
                val y1 = mRollValues[i][3]
                val x2 = mRollValues[i][2] + rollAreaWidth
                val y2 = mRollValues[i][3] + rollAreaWidth
                val rect = Rect(x1, y1, x2, y2)
                if (rect.contains(touchPoint.x, touchPoint.y))
                    return mRollValues[i][0]
            }
        }
        for (i in 0 until diceCnt) {
            if (mHoldValues[i][1] == 1)
            // state: visible & selectable
            {
                val x1 = mHoldValues[i][2]
                val y1 = mHoldValues[i][3]
                val x2 = mHoldValues[i][2] + mHoldValues[i][4]
                val y2 = mHoldValues[i][3] + mHoldValues[i][5]
                val rect = Rect(x1, y1, x2, y2)
                if (rect.contains(touchPoint.x, touchPoint.y))
                    return mHoldValues[i][0]
            }
        }
        for (i in 0 until diceCnt) {

            if (mControlValues[i][1] == 1)
            // state: visible & selectable
            {
                val x1 = mControlValues[i][2]
                val y1 = mControlValues[i][3]
                val x2 = mControlValues[i][2] + mControlValues[i][4]
                val y2 = mControlValues[i][3] + mControlValues[i][5]

//                Log.i(TAG, "getIdFromTouchPoint(), x1: $x1, y1: $y1, x2: $x2, y2: $y2")
//                Log.i(TAG, "getIdFromTouchPoint(), resId: ${mControlValues[i][0]}")

                val rect = Rect(x1, y1, x2, y2)
                if (rect.contains(touchPoint.x, touchPoint.y))
                    return mControlValues[i][0]
            }
        }
        return 0
    }

    fun getHoldIdFromBoard(boardHoldId: Int): Int {
        return mHoldIdx[boardHoldId]
    }

    private fun setRollValues(roll: IntArray, rollValues: Array<IntArray>, isSelectable: Boolean, initRollValues: Boolean) {
        var r: Int
        for (i in 0 until diceCnt) {

            //Log.i(TAG, "bWidth: " + mBoardWidth + ", bHeight: " + mBoardHeight + ", x: " + pt.x + ", y: " + pt.y + ", mDiceWidth: " + mDiceWidth);

            rollValues[i][1] = 0
            if (roll[i] >= 0) {
                if (isSelectable)
                    rollValues[i][1] = 1
                else
                    rollValues[i][1] = 2
            }
            if (initRollValues) {
                r = mRand.nextInt(360) // rotation
                val pt = getFreeRollPoint(i, rollValues)
                rollValues[i][2] = pt.x
                rollValues[i][3] = pt.y
                rollValues[i][4] = mDiceSize
                rollValues[i][5] = mDiceSize
                rollValues[i][6] = r
            }
        }
    }

    private fun setHoldValues(hold: IntArray, holdIdx: IntArray, holdValues: Array<IntArray>, isSelectable: Boolean) {
        var idx = 0
        for (j in holdIdx.indices) {
            holdIdx[j] = -1
        }
        // sort hold values
        val valuesHold = IntArray(6)
        val valuesRoll = IntArray(6)
        for (j in valuesHold.indices) {
            valuesHold[j] = 0
            valuesRoll[j] = 0
        }
        for (i in hold.indices) {
            if (hold[i] >= 0)
                valuesHold[hold[i]] = valuesHold[hold[i]] + 1
        }
        var max = 0
        for (i in valuesHold.indices) {
            if (valuesHold[i] + valuesRoll[i] > max)
                max = valuesHold[i] + valuesRoll[i]
        }
        var isStraight = false
        if ((max == 1) and !((valuesHold[0] + valuesRoll[0] == 1) and (valuesHold[5] + valuesRoll[5] == 1)))
            isStraight = true
        if (isStraight)
        // 9...A
        {
            for (i in 0..5) {
                for (j in hold.indices) {
                    if (hold[j] == i) {
                        holdIdx[idx] = j
                        idx++
                    }
                }
            }
        } else
        // A...9
        {
            for (i in 5 downTo 1) {
                for (h in valuesHold.indices.reversed()) {
                    if (valuesHold[h] == i) {
                        for (j in hold.indices) {
                            if (hold[j] == h) {
                                holdIdx[idx] = j
                                idx++
                            }
                        }
                    }
                }

            }
        }
        var x = mBoardBorderWidth
        val y = mBoardHeight - mBoardBorderWidth - mDiceSize
        for (i in 0 until diceCnt) {
            holdValues[i][1] = 0
            if (holdIdx[i] >= 0) {
                if (isSelectable)
                    holdValues[i][1] = 1
                else
                    holdValues[i][1] = 2
            } else {
                if (isSelectable) {
                    holdValues[i][0] = R.id.dbHoldFast
                    holdValues[i][1] = 1
                }
            }
            holdValues[i][2] = x
            holdValues[i][3] = y
            holdValues[i][4] = mDiceSize
            holdValues[i][5] = mDiceSize
            holdValues[i][6] = 0
            x += mDiceSize
        }
    }

    private fun setDoubleValues(double1: IntArray, double1Values: Array<IntArray>) {
        // sort doubleValues
        if (double1[0] >= 0) {
            val valuesDouble1 = IntArray(6)
            val doubleNew1 = IntArray(5)
            for (j in valuesDouble1.indices) {
                valuesDouble1[j] = 0
            }
            for (i in double1.indices) {
                if (double1[i] >= 0)
                    valuesDouble1[double1[i]] = valuesDouble1[double1[i]] + 1
            }
            var max = 0
            var idx = 0
            for (i in valuesDouble1.indices) {
                if (valuesDouble1[i] > max)
                    max = valuesDouble1[i]
            }
            var isStraight = false
            if ((max == 1) and !((valuesDouble1[0] == 1) and (valuesDouble1[5] == 1)))
                isStraight = true
            if (isStraight)
            // 9...A
            {
                for (i in 0..5) {
                    for (j in double1.indices) {
                        if (double1[j] == i) {
                            doubleNew1[idx] = double1[j]
                            idx++
                        }
                    }
                }
            } else
            // A...9
            {
                for (i in 5 downTo 1) {
                    for (h in valuesDouble1.indices.reversed()) {
                        if (valuesDouble1[h] == i) {
                            for (j in double1.indices) {
                                if (double1[j] == h) {
                                    doubleNew1[idx] = double1[j]
                                    idx++
                                }
                            }
                        }
                    }

                }
            }
            mDouble1 = doubleNew1
        }
        // setDoubleValues
        var x = mBoardWidth - mBoardBorderWidth - mHoldAreaWidth
        val y = mBoardBorderWidth
        for (i in 0 until diceCnt) {
            double1Values[i][1] = 2
            if (double1[0] < 0)
                double1Values[i][1] = 0
            double1Values[i][2] = x
            double1Values[i][3] = y
            double1Values[i][4] = mDiceSize
            double1Values[i][5] = mDiceSize
            double1Values[i][6] = 0
            x += mDiceSize
        }
    }

    private fun setControlValues(isSingle: Boolean, doubleState: Int,
                                 playerInfo: String, controlValues: Array<IntArray>) {

//        Log.i(TAG, "setControlValues(), isSingle: $isSingle, doubleState: $doubleState, playerInfoDouble: $playerInfoDouble")
//        Log.i(TAG, "setControlValues(), bW: $mBoardWidth, bH: $mBoardWidth, bbW: $mBoardBorderWidth, haW: $mHoldAreaWidth, haH: $mHoldAreaHeight, dS: $mDiceSize")

        controlValues[0][1] = 0
        controlValues[1][1] = 0
        controlValues[2][1] = 0
        controlValues[3][1] = 0 // init state
        if (!isSingle)
        {

//            Log.i(TAG, "setControlValues(), [1]")

            controlValues[1][1] = doubleState
            controlValues[1][2] = mBoardWidth - mBoardBorderWidth - mHoldAreaWidth - 20
            controlValues[1][3] = mBoardBorderWidth + 10
            controlValues[1][4] = mHoldAreaWidth
            controlValues[1][5] = mHoldAreaHeight
            controlValues[1][6] = 0
        }
        if (playerInfo != "") {

//            Log.i(TAG, "setControlValues(), [2]")

            controlValues[2][1] = 1
            controlValues[2][2] = mBoardWidth - 200
            controlValues[2][3] = mBoardHeight - mBoardBorderWidth - mDiceSize * 2 - 10
            controlValues[2][4] = 180
            controlValues[2][5] = mDiceSize * 60 / 100   // 60 % of dice tableHeight
            controlValues[2][6] = 0
        }
        if (!isSingle) {

//            Log.i(TAG, "setControlValues(), [3]")

            controlValues[3][1] = doubleState
            controlValues[3][2] = mBoardWidth - 200
            controlValues[3][3] = mBoardBorderWidth + mDiceSize + 40
            controlValues[3][4] = 180
            controlValues[3][5] = mDiceSize * 60 / 100   // 60 % of dice tableHeight
            controlValues[3][6] = 0
        }
    }

    private fun getFreeRollPoint(rollId: Int, rollValues: Array<IntArray>): Point {
        val numberOfHoldArea = if (mIsSingle) 1 else 2

        val gridWidth = sqrt((mDiceSize * mDiceSize + mDiceSize * mDiceSize).toDouble()).toInt()
        val gridX = (mBoardWidth - 2 * mBoardBorderWidth) / gridWidth
        val gridY = (mBoardHeight - 2 * mBoardBorderWidth - numberOfHoldArea * mDiceSize) / gridWidth

        // 20190223, java.lang.NegativeArraySizeException:
        val gridPoints = arrayOfNulls<Point>(gridX * gridY)
        if (gridX < 0 || gridY < 0)
            return Point(mBoardBorderWidth, mBoardBorderWidth)

        var gridPointsSize = 0
        for (k in 0 until gridX)
        // check all possible grid points if free
            for (j in 0 until gridY) {
                val xGridPoint = mBoardBorderWidth + k * gridWidth
                val yGridPoint = mBoardBorderWidth + (numberOfHoldArea - 1) * mDiceSize + j * gridWidth
                var isFree = true
                for (i in 0 until rollId)
                // check previous roll dices
                {
                    if (rollValues[i][2] == xGridPoint && rollValues[i][3] == yGridPoint) {
                        isFree = false
                        break
                    }
                }
                if (isFree)
                    gridPoints[gridPointsSize++] = Point(xGridPoint, yGridPoint)
            }
        if (gridPointsSize > 0) {
            val n = mRand.nextInt(gridPointsSize)
            gridPoints[n]?.let { value -> return value}
            return Point(mBoardBorderWidth, mBoardBorderWidth + (numberOfHoldArea - 1) * mDiceSize)
        } else
            return Point(mBoardBorderWidth, mBoardBorderWidth + (numberOfHoldArea - 1) * mDiceSize)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

//        Log.i(TAG, "onDraw(), diceSize: $diceSize, mDiceSize: $mDiceSize, mRollValues[0][4]: ${mRollValues[0][4]}")

        drawBoard(canvas)
        if (!isInit && mRollValues[0][4] > 0)
            drawImages(canvas)
        isInit = false
    }

    private fun drawBoard(canvas: Canvas) {
        mPaint!!.style = Paint.Style.FILL
        mPaint!!.color = ContextCompat.getColor(context, R.color.colorBoard)
        mRect.set(0, 0, mBoardWidth - 1, mBoardHeight - 1)
        canvas.drawRect(mRect, mPaint!!)

        mPaint!!.style = Paint.Style.STROKE
        mPaint!!.color = ContextCompat.getColor(context, R.color.colorBoardBorder)
        mPaint!!.strokeWidth = (mBoardBorderWidth * 2).toFloat()
        mRect.set(0, 0, mBoardWidth - 1, mBoardHeight - 1)
        canvas.drawRect(mRect, mPaint!!)

        if (mColValues != null) {
            mPaint!!.reset()
            mPaint!!.color = ContextCompat.getColor(context, R.color.colorLabel)
            var textSize = mDiceSize

            if (diceSize == 3)
                textSize = (textSize * 0.6f).toInt()
            mPaint!!.textSize = textSize.toFloat()
            var bounds = Rect()
            val textEscalero = resources.getString(R.string.escalero)
            var text = ""
            val l = textEscalero.length
            for (i in 0 until l) {
                text = text + textEscalero[i] + " "
            }
            mPaint!!.getTextBounds(text, 0, text.length, bounds)
            var x = mBoardWidth / 2 - bounds.width() / 2
            var y = mBoardHeight / 2 - bounds.height() / 2 + (mDiceSize * 0.6f).toInt()
            canvas.drawText(text, x.toFloat(), y.toFloat(), mPaint!!)

            mPaint!!.textSize = textSize * 0.6f
            bounds = Rect()

            var gameType = resources.getString(R.string.typeSingle)
            if (!mIsSingle)
                gameType = resources.getString(R.string.typeDouble)
            val game = "$gameType ($mColValues)"
            mPaint!!.getTextBounds(game, 0, game.length, bounds)
            x = mBoardWidth / 2 - bounds.width() / 2
            y = mBoardHeight / 2 - bounds.height() / 2 + (mDiceSize * 0.6f).toInt() + (mDiceSize * 0.6f).toInt()
            canvas.drawText(game, x.toFloat(), y.toFloat(), mPaint!!)

            if (mOnlinePlayers.isNotEmpty()) {
                mPaint!!.textSize = textSize * 0.6f
                bounds = Rect()

                val strResult = mOnlinePlayers.split("\n".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                for (i in strResult.indices) {
                    mPaint!!.getTextBounds(strResult[i], 0, strResult[i].length, bounds)
                    x = mBoardWidth / 2 - bounds.width() / 2
                    y = mBoardHeight / 2 - bounds.height() / 2 + (mDiceSize * 0.6f).toInt()
                    when (strResult.size) {
                        1 -> {
                            y -= textSize + (mDiceSize * 0.2f).toInt()
                        }
                        2 -> {
                            y -=    if (i == 0)
                                textSize + (mDiceSize * 0.8f).toInt()
                            else
                                textSize + (mDiceSize * 0.2f).toInt()

                        }
                    }

                    canvas.drawText(strResult[i], x.toFloat(), y.toFloat(), mPaint!!)
                }

            }

        }
    }

    private fun drawImages(canvas: Canvas) {
        mPaint!!.isAntiAlias = true
        // hold area border button
        for (i in 0 until diceCnt) {
            if (mHoldValues[i][4] > 0) {
                var bm = BitmapFactory.decodeResource(resources, R.drawable.button_border)
                bm = Bitmap.createScaledBitmap(bm, mHoldValues[i][4], mHoldValues[i][5], false)
                canvas.drawBitmap(bm, mHoldValues[i][2].toFloat(), mHoldValues[i][3].toFloat(), mPaint)
            }
        }
        // double area border button
        if (!mIsSingle) {
            for (i in 0 until diceCnt) {
                if (mDouble1Values[i][4] > 0) {
                    var bm = BitmapFactory.decodeResource(resources, R.drawable.button_border)
                    bm = Bitmap.createScaledBitmap(bm, mDouble1Values[i][4], mDouble1Values[i][5], false)
                    canvas.drawBitmap(bm, mDouble1Values[i][2].toFloat(), mDouble1Values[i][3].toFloat(), mPaint)
                }
            }
        }
        // roll
        for (i in 0 until diceCnt) {
            if ((mRollValues[i][1] != 0) and (mRoll[i] >= 0))
            // state
            {
                var bm = BitmapFactory.decodeResource(resources, diceIcons!![mRoll[i]])
                bm = Bitmap.createScaledBitmap(bm, mRollValues[i][4], mRollValues[i][5], false)
                val matrix = Matrix()
                matrix.preRotate(mRollValues[i][6].toFloat())
                val bitmap = Bitmap.createBitmap(bm, 0, 0, mRollValues[i][4], mRollValues[i][5], matrix, false)
                canvas.drawBitmap(bitmap, mRollValues[i][2].toFloat(), mRollValues[i][3].toFloat(), mPaint)
            }
        }

        // hold
        for (i in 0 until diceCnt) {
            if ((mHoldValues[i][1] != 0) and (mHoldIdx[i] >= 0)) {
                if (mHold[mHoldIdx[i]] >= 0) {
                    var bm = BitmapFactory.decodeResource(resources, diceIcons!![mHold[mHoldIdx[i]]])
                    bm = Bitmap.createScaledBitmap(bm, mHoldValues[i][4], mHoldValues[i][5], false)
                    canvas.drawBitmap(bm, mHoldValues[i][2].toFloat(), mHoldValues[i][3].toFloat(), mPaint)
                }
            }
        }

        // double
        if (!mIsSingle and (mControlValues[1][1] != 0)) {
            for (i in 0 until diceCnt) {
                if ((mDouble1Values[i][1] != 0) and (mDouble1[i] >= 0))
                // state
                {
                    var bm = BitmapFactory.decodeResource(resources, diceIcons!![mDouble1[i]])
                    bm = Bitmap.createScaledBitmap(bm, mDouble1Values[i][4], mDouble1Values[i][5], false)
                    canvas.drawBitmap(bm, mDouble1Values[i][2].toFloat(), mDouble1Values[i][3].toFloat(), mPaint)
                }
            }
        }
        // controls
        // btn info playerName
        if (mControlValues[2][1] != 0) {
            mPaint = Paint()
            mPaint!!.color = Color.BLACK
            mPaint!!.textSize = mControlValues[2][5].toFloat()
            val bounds = Rect()
            mPaint!!.getTextBounds(mPlayerInfo, 0, mPlayerInfo.length, bounds)
            val x = mBoardWidth - mBoardBorderWidth * 2 - bounds.width()
            val y = mBoardHeight - mBoardBorderWidth * 2
            val b = 2
            var color = ContextCompat.getColor(context, R.color.colorPlayerA)
            if (mPlayerToMove == 'B')
                color = ContextCompat.getColor(context, R.color.colorPlayerB)
            if (mPlayerToMove == 'C')
                color = ContextCompat.getColor(context, R.color.colorPlayerC)
            val paintRect = Paint()
            paintRect.style = Paint.Style.FILL
            paintRect.color = color
            mRect.set(x - b, y - bounds.height() - b, x + bounds.width() + b, y + b)
            canvas.drawRect(mRect, paintRect)
            canvas.drawText(mPlayerInfo, x.toFloat(), y.toFloat(), mPaint!!)
        }
        // info double first playerName
        if (mControlValues[3][1] != 0) {
            mPaint = Paint()
            mPaint!!.color = Color.BLACK
            mPaint!!.textSize = mControlValues[3][5].toFloat()
            val bounds = Rect()
            mPaint!!.getTextBounds(mPlayerInfoDouble, 0, mPlayerInfoDouble.length, bounds)
            val x = mBoardWidth - mBoardBorderWidth * 2 - bounds.width()
            val y = mBoardBorderWidth + mDiceSize
            val b = 2
            var color = ContextCompat.getColor(context, R.color.colorPlayerA)
            if (mPlayerToMove == 'B')
                color = ContextCompat.getColor(context, R.color.colorPlayerB)
            if (mPlayerToMove == 'C')
                color = ContextCompat.getColor(context, R.color.colorPlayerC)
            val paintRect = Paint()
            paintRect.style = Paint.Style.FILL
            paintRect.color = color
            mRect.set(x - b, y - bounds.height() - b, x + bounds.width() + b, y + b)
            canvas.drawRect(mRect, paintRect)
            canvas.drawText(mPlayerInfoDouble, x.toFloat(), y.toFloat(), mPaint!!)
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val widthMode = MeasureSpec.getMode(widthMeasureSpec)
        val widthSize = MeasureSpec.getSize(widthMeasureSpec)
        val heightMode = MeasureSpec.getMode(heightMeasureSpec)
        val heightSize = MeasureSpec.getSize(heightMeasureSpec)
        val desiredWidth = if (widthSize < heightSize) widthSize else heightSize / 3 * 4 // aspect ratio 4:3

        val width =  when {
            (widthMode == MeasureSpec.EXACTLY)
                -> widthSize
            (widthMode == MeasureSpec.AT_MOST)
                -> min(desiredWidth, widthSize)
            else
                -> desiredWidth
       }

        //Measure Height
        val desiredHeight = if (widthSize < heightSize) heightSize else widthSize / 4 * 3 // aspect ratio 4:3

        val height = when {
            (heightMode == MeasureSpec.EXACTLY)
                -> heightSize
            (heightMode == MeasureSpec.AT_MOST)
                -> min(desiredHeight, heightSize)
            else
                -> desiredHeight
        }

        //Log.i(TAG, "onMeasure(), width: " + width + ", height: " + height);
        //MUST CALL THIS
        setMeasuredDimension(width, height)

    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {

        if ((w > 0) and (h > 0)) {
            mBoardWidth = w
            mBoardHeight = h
            mBoardBorderWidth = w / 40 // 2,5% border
            var mDiceWidthWidth = (mBoardWidth - 2 * mBoardBorderWidth) / SIZE1_SMALL
            var mDiceWidthHeight = ((mBoardHeight - 2 * mBoardBorderWidth).toDouble() / SIZE2_SMALL).toInt()
            mDiceSizeSmall = min(mDiceWidthWidth, mDiceWidthHeight)
            mDiceWidthWidth = (mBoardWidth - 2 * mBoardBorderWidth) / SIZE1_MEDIUM
            mDiceWidthHeight = ((mBoardHeight - 2 * mBoardBorderWidth).toDouble() / SIZE2_MEDIUM).toInt()
            mDiceSizeMedium = min(mDiceWidthWidth, mDiceWidthHeight)
            mDiceWidthWidth = (mBoardWidth - 2 * mBoardBorderWidth) / SIZE1_LARGE
            mDiceWidthHeight = ((mBoardHeight - 2 * mBoardBorderWidth).toDouble() / SIZE2_LARGE).toInt()
            mDiceSizeLarge = min(mDiceWidthWidth, mDiceWidthHeight)
            when (diceSize) {
                1 -> mDiceSize = mDiceSizeSmall
                2 -> mDiceSize = mDiceSizeMedium
                3 -> mDiceSize = mDiceSizeLarge
            }
            mHoldAreaWidth = 5 * mDiceSize
            mHoldAreaHeight = mDiceSize
            //Log.i(TAG, "onSizeChanged(), mBoar/dWidth: " + mBoardWidth + ", mBoardHeight: " + mBoardHeight + ", mDiceSize: " + mDiceSize);
        }
    }

    companion object {
//        const val TAG = "BoardView"
        const val SIZE1_SMALL = 8
        const val SIZE1_MEDIUM = 7
        const val SIZE1_LARGE = 6
        const val SIZE2_SMALL = 5.5
        const val SIZE2_MEDIUM = 4.5
        const val SIZE2_LARGE = 3.5
    }

}
