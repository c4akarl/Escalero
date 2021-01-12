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

import android.content.Context
import android.content.res.Configuration
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import kotlin.math.min

class TableView(context: Context, attrs: AttributeSet) : View(context, attrs) {

    private lateinit var ed: EscaleroData
    private var isOnline = false
    private var isOnlineMatch = false
    private var density: Double = 0.toDouble()
    // table values
    private lateinit var gridValues: Array<String>  // EscaleroData.gridCurrent

    internal var orientation: Int = 0
    private var displayWidth = 0
    private var displayHeight = 0
    private var tablePortraitHeight = 0.6
    private var tableLandscapeWidth = 0.45

    private var tableWidth: Int = 0
    private var tableHeight: Int = 0
    private var tableCol: Int = 0
    private var tableRow: Int = 0
    private var cellWidth: Int = 0
    private var cellHeight: Int = 0
    // draw values
    private val mPaint: Paint
    private lateinit var bmDiceIcon: Bitmap
    private var strokeWidth = 1
    private var strokeWidthBold = 3
    private val strokeWidthFont = 1
    private var cellCnt: Int = 0    // number of cells
    private lateinit var cellValues: Array<IntArray>     // [cellCnt][values]

    init {
        density = resources.displayMetrics.density.toDouble()
        mPaint = Paint()
    }

    fun initTable(escaleroData: EscaleroData, orientation: Int, displayWidth: Int, displayHeight: Int) {
        ed = escaleroData
        this.orientation = orientation
        this.displayWidth = displayWidth
        this.displayHeight = displayHeight

        //Log.i(TAG, "initTable(), orientation: " + orientation + ", displayWidth: " + displayWidth + ", displayHeight: " + displayHeight);
//        Log.i(TAG, "initTable(), ed.isFlipScreen: ${ed.isFlipScreen}")

        gridValues = ed.gridValues
        tableCol = ed.numColumns
        tableRow = 12
        cellCnt = tableCol * tableRow
        if ((tableWidth > 0) and (tableCol > 0)) {
            cellWidth = tableWidth / tableCol
            cellHeight = tableHeight / tableRow
        }
        initCellValues()
    }

    private fun initCellValues() {
        cellValues = Array(cellCnt) { IntArray(VALUES) }
        var x = 0
        var y = 0
        for (i in 0 until cellCnt) {
            cellValues[i][0] = x
            cellValues[i][1] = y
            cellValues[i][2] = cellWidth
            cellValues[i][3] = cellHeight
            x += cellWidth
            if (i > 0) {
                if ((i + 1) % tableCol == 0)
                {
                    x = 0
                    y += cellHeight
                }
            }
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        for (i in ed.gridCurrent.indices) {

            //Log.i(TAG, "onDraw(), grid position: " + i  + ": " + ed.gridCurrent[i] + ", value: " + gridValues[i]);

            val diceValue = Character.getNumericValue(ed.gridCurrent[i][1])
            if ((diceValue <= 5) and ed.gridCurrent[i].startsWith("T"))
                drawDice(i, canvas)        // images 9...A
            else {
                if (ed.gridCurrent[i].startsWith("A")
                        or ed.gridCurrent[i].startsWith("B")
                        or ed.gridCurrent[i].startsWith("C"))
                    drawEntry(i, canvas)   // entry(dice result)
                else
                    drawText(i, canvas)    // label, sumation and special buttons(corners)
            }
        }
    }

    private fun drawDice(position: Int, canvas: Canvas) {
        val gridId = ed.gridCurrent[position]
        val diceValue = Character.getNumericValue(gridId[1])
        when (diceValue) {
            0 -> bmDiceIcon = BitmapFactory.decodeResource(resources, ed.icon1).copy(Bitmap.Config.ARGB_8888, true)
            1 -> bmDiceIcon = BitmapFactory.decodeResource(resources, ed.icon2).copy(Bitmap.Config.ARGB_8888, true)
            2 -> bmDiceIcon = BitmapFactory.decodeResource(resources, ed.icon3).copy(Bitmap.Config.ARGB_8888, true)
            3 -> bmDiceIcon = BitmapFactory.decodeResource(resources, ed.icon4).copy(Bitmap.Config.ARGB_8888, true)
            4 -> bmDiceIcon = BitmapFactory.decodeResource(resources, ed.icon5).copy(Bitmap.Config.ARGB_8888, true)
            5 -> bmDiceIcon = BitmapFactory.decodeResource(resources, ed.icon6).copy(Bitmap.Config.ARGB_8888, true)
        }
        if (diceValue < 6) {
            val bitmap = Bitmap.createBitmap(cellWidth, cellHeight, Bitmap.Config.ARGB_8888)
            val cellCanvas = Canvas(bitmap)
            val paint = Paint()
            mPaint.color = ContextCompat.getColor(context, R.color.colorGrey)
            paint.style = Paint.Style.FILL
            val rect = RectF(0f, 0f, cellCanvas.width.toFloat(), cellCanvas.height.toFloat())
            cellCanvas.drawRect(rect, paint)
            var st = strokeWidthBold
            if (cellWidth < 100)
            // for HTC baby
                st = 0
            //Lars, 19.06.2017
            //            cellCanvas.drawBitmap(bmDiceIcon, null, new Rect(st, st, cellWidth - st, cellHeight - st), null);
            cellCanvas.drawBitmap(bmDiceIcon, null, Rect(st, st, cellWidth - st - 1, cellHeight - st - 1), null)

            // draw frame
            paint.style = Paint.Style.STROKE
            paint.color = ContextCompat.getColor(context, R.color.colorTableStroke)
            paint.strokeWidth = strokeWidth.toFloat()
            cellCanvas.drawLine(0f, 0f, 0f, cellCanvas.height.toFloat(), paint) // left
            paint.color = ContextCompat.getColor(context, R.color.colorTableStrokeBold)
            paint.strokeWidth = strokeWidthBold.toFloat()
            cellCanvas.drawLine((cellCanvas.width - 1).toFloat(), 0f, (cellCanvas.width - 1).toFloat(), cellCanvas.height.toFloat(), paint) // right
            if (diceValue == 0) {
                paint.color = ContextCompat.getColor(context, R.color.colorTableStrokeBold)
                paint.strokeWidth = strokeWidthBold.toFloat()
            } else {
                paint.color = ContextCompat.getColor(context, R.color.colorTableStroke)
                paint.strokeWidth = strokeWidth.toFloat()
            }
            cellCanvas.drawLine(0f, 0f, cellCanvas.width.toFloat(), 0f, paint) // top
            if (diceValue == 5) {
                paint.color = ContextCompat.getColor(context, R.color.colorTableStrokeBold)
                paint.strokeWidth = strokeWidthBold.toFloat()
            } else {
                paint.color = ContextCompat.getColor(context, R.color.colorTableStroke)
                paint.strokeWidth = strokeWidth.toFloat()
            }
            cellCanvas.drawLine(0f, (cellCanvas.height - 1).toFloat(), (cellCanvas.width - 1).toFloat(), (cellCanvas.height - 1).toFloat(), paint) // bottom
            canvas.drawBitmap(bitmap, cellValues[position][0].toFloat(), cellValues[position][1].toFloat(), mPaint)

        }
    }

    private fun drawEntry(position: Int, canvas: Canvas) {
        // image background
        val gridId = ed.gridCurrent[position]
        val row = Character.getNumericValue(gridId[2])
        var isPlayerMove = false
        if (ed.gridCurrent[position][0] == ed.playerToMove)
            isPlayerMove = true
        // image shape
        var imgCol = ContextCompat.getColor(context, R.color.colorPlayerA)
        if (ed.gridCurrent[position].startsWith("B"))
            imgCol = ContextCompat.getColor(context, R.color.colorPlayerB)
        if (ed.gridCurrent[position].startsWith("C"))
            imgCol = ContextCompat.getColor(context, R.color.colorPlayerC)
        if (!isPlayerMove && !ed.isGameOver)
            imgCol = ContextCompat.getColor(context, R.color.colorWhite)
        if (!isOnline and !isOnlineMatch and isPlayerMove and (position == ed.selectedGridItem))
            imgCol = ContextCompat.getColor(context, R.color.colorLastEntry)

        val bitmap = Bitmap.createBitmap(cellWidth, cellHeight, Bitmap.Config.ARGB_8888)
        val cellCanvas = Canvas(bitmap)
        val paint = Paint()
        paint.color = imgCol
        paint.style = Paint.Style.FILL
        val rect = RectF(0f, 0f, cellCanvas.width.toFloat(), cellCanvas.height.toFloat())
        cellCanvas.drawRect(rect, paint)

        if (isPlayerMove and (gridValues[position] != "-"))
            paint.color = Color.BLACK
        if (isPlayerMove and (gridValues[position] == "-"))
            paint.color = ContextCompat.getColor(context, R.color.colorLastEntry)
        if (!isPlayerMove)
            paint.color = ContextCompat.getColor(context, R.color.colorNoEntry)
        if ((ed.selectedGridItem >= 0) and (ed.selectedGridItem == position))
            paint.color = ContextCompat.getColor(context, R.color.colorLastEntry)
        if (ed.isGameOver)
            paint.color = Color.BLACK
        // text (result)
//        val tf = Typeface.createFromAsset(context.assets, "fonts/HandWritten.ttf")
//        paint.typeface = tf
        val ch = cellCanvas.height
        val ts = (ch * 0.6f).toInt()
        paint.textSize = ts.toFloat()
        paint.style = Paint.Style.FILL_AND_STROKE
        paint.strokeWidth = strokeWidthFont.toFloat()
        paint.textAlign = Paint.Align.CENTER
        val x = cellCanvas.width / 2
        val y = (cellCanvas.height / 2 - (paint.descent() + paint.ascent()) / 2).toInt()
        //Log.i(TAG, "drawEntry(), cw: " + cw + ", ch: " + ch + ", ts: " + ts + ", x: " + x + ", y: " + y);
        //Lars, 19.06.2017
        paint.flags = Paint.ANTI_ALIAS_FLAG
        if (gridValues[position] != "-")
            cellCanvas.drawText(gridValues[position], x.toFloat(), y.toFloat(), paint)

        // draw frame
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = strokeWidth.toFloat()
        paint.color = ContextCompat.getColor(context, R.color.colorTableStroke)
        cellCanvas.drawLine(0f, 0f, 0f, cellCanvas.height.toFloat(), paint) // left
        cellCanvas.drawLine((cellCanvas.width - 1).toFloat(), 0f, (cellCanvas.width - 1).toFloat(), cellCanvas.height.toFloat(), paint) // right
        cellCanvas.drawLine(0f, 0f, cellCanvas.width.toFloat(), 0f, paint) // top
        cellCanvas.drawLine(0f, (cellCanvas.height - 1).toFloat(), (cellCanvas.width - 1).toFloat(), (cellCanvas.height - 1).toFloat(), paint) // bottom
        // draw column delimiter
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = strokeWidthBold.toFloat()
        paint.color = ContextCompat.getColor(context, R.color.colorTableStrokeBold)
        // vertical (left, right)
        if (!ed.isPlayerColumn)
        // column / playerName
        {
            if (ed.gridCurrent[position].startsWith("A0")
                    or ed.gridCurrent[position].startsWith("A1")
                    or ed.gridCurrent[position].startsWith("A2")) {
                cellCanvas.drawLine(0f, 0f, 0f, (cellCanvas.height - 1).toFloat(), paint) // left
            }
            var lastPlayer = "B"
            if (ed.playerNumber == 3)
                lastPlayer = "C"
            if (ed.gridCurrent[position].startsWith(lastPlayer))
                cellCanvas.drawLine((cellCanvas.width - 1).toFloat(), 0f, (cellCanvas.width - 1).toFloat(), (cellCanvas.height - 1).toFloat(), paint) // right
        } else
        // playerName / column
        {
            if (ed.gridCurrent[position].startsWith("A0")
                    or ed.gridCurrent[position].startsWith("B0")
                    or ed.gridCurrent[position].startsWith("C0")) {
                cellCanvas.drawLine(0f, 0f, 0f, (cellCanvas.height - 1).toFloat(), paint) // left
            }
            if (gridId[1] == '2')
                cellCanvas.drawLine((cellCanvas.width - 1).toFloat(), 0f, (cellCanvas.width - 1).toFloat(), (cellCanvas.height - 1).toFloat(), paint) // right
        }
        // horizontal (top, bottom)
        if ((row == 0) or (row == 6)) cellCanvas.drawLine(0f, 0f, (cellCanvas.width - 1).toFloat(), 0f, paint) // top
        if ((row == 5) or (row == 9)) cellCanvas.drawLine(0f, (cellCanvas.height - 1).toFloat(), (cellCanvas.width - 1).toFloat(), (cellCanvas.height - 1).toFloat(), paint) // bottom

        canvas.drawBitmap(bitmap, cellValues[position][0].toFloat(), cellValues[position][1].toFloat(), mPaint)

    }

    private fun drawText(position: Int, canvas: Canvas) {
        val bitmap = Bitmap.createBitmap(cellWidth, cellHeight, Bitmap.Config.ARGB_8888)
        val cellCanvas = Canvas(bitmap)
        val paint = Paint()
        if (ed.gridCurrent[position].startsWith("T"))
            paint.color = ContextCompat.getColor(context, R.color.colorGrey)
        if (ed.gridCurrent[position].startsWith("N"))
            paint.color = ContextCompat.getColor(context, R.color.colorGrey)
        if (ed.gridCurrent[position].startsWith("LA") or ed.gridCurrent[position].startsWith("SA"))
            if (ed.gridCurrent[position][1] == ed.playerToMove)
                paint.color = ContextCompat.getColor(context, R.color.colorPlayerAS)
            else
                paint.color = Color.WHITE
        if (ed.gridCurrent[position].startsWith("LB") or ed.gridCurrent[position].startsWith("SB"))
            if (ed.gridCurrent[position][1] == ed.playerToMove)
                paint.color = ContextCompat.getColor(context, R.color.colorPlayerBS)
            else
                paint.color = Color.WHITE
        if (ed.gridCurrent[position].startsWith("LC") or ed.gridCurrent[position].startsWith("SC"))
            if (ed.gridCurrent[position][1] == ed.playerToMove)
                paint.color = ContextCompat.getColor(context, R.color.colorPlayerCS)
            else
                paint.color = Color.WHITE
        if (ed.gridCurrent[position].startsWith("X"))
            paint.color = ContextCompat.getColor(context, R.color.colorXS)
        paint.style = Paint.Style.FILL
        val rect = RectF(0f, 0f, cellCanvas.width.toFloat(), cellCanvas.height.toFloat())
        cellCanvas.drawRect(rect, paint)
        // draw delimiter
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = strokeWidthBold.toFloat()
        paint.color = ContextCompat.getColor(context, R.color.colorTableStrokeBold)
        // vertical (left, right)
        if (ed.gridCurrent[position].startsWith("X01")) cellCanvas.drawLine(0f, 0f, 0f, (cellCanvas.height - 1).toFloat(), paint) // left
        if (ed.gridCurrent[position].startsWith("N")) cellCanvas.drawLine(0f, 0f, 0f, (cellCanvas.height - 1).toFloat(), paint) // left
        if (ed.gridCurrent[position].startsWith("X03")) cellCanvas.drawLine(0f, 0f, 0f, (cellCanvas.height - 1).toFloat(), paint) // left
        if (ed.gridCurrent[position].startsWith("X00")) cellCanvas.drawLine((cellCanvas.width - 1).toFloat(), 0f, (cellCanvas.width - 1).toFloat(), (cellCanvas.height - 1).toFloat(), paint) // right
        if (ed.gridCurrent[position].startsWith("T")) cellCanvas.drawLine((cellCanvas.width - 1).toFloat(), 0f, (cellCanvas.width - 1).toFloat(), (cellCanvas.height - 1).toFloat(), paint) // right
        if (ed.gridCurrent[position].startsWith("X02")) cellCanvas.drawLine((cellCanvas.width - 1).toFloat(), 0f, (cellCanvas.width - 1).toFloat(), (cellCanvas.height - 1).toFloat(), paint) // right
        if (!ed.isPlayerColumn)
        // column / playerName
        {
            if (ed.gridCurrent[position].startsWith("LA0")
                    or ed.gridCurrent[position].startsWith("LA1")
                    or ed.gridCurrent[position].startsWith("LA2")
                    or ed.gridCurrent[position].startsWith("SA0")
                    or ed.gridCurrent[position].startsWith("SA1")
                    or ed.gridCurrent[position].startsWith("SA2")) {
                cellCanvas.drawLine(0f, 0f, 0f, (cellCanvas.height - 1).toFloat(), paint) // left
            }
            var lastPlayer = "B"
            if (ed.playerNumber == 3)
                lastPlayer = "C"
            if (ed.gridCurrent[position].startsWith("L$lastPlayer") or ed.gridCurrent[position].startsWith("S$lastPlayer"))
                cellCanvas.drawLine((cellCanvas.width - 1).toFloat(), 0f, (cellCanvas.width - 1).toFloat(), (cellCanvas.height - 1).toFloat(), paint) // right
        } else
        // playerName / column
        {
            if (ed.gridCurrent[position].startsWith("LA0")
                    or ed.gridCurrent[position].startsWith("LB0")
                    or ed.gridCurrent[position].startsWith("LC0")
                    or ed.gridCurrent[position].startsWith("SA0")
                    or ed.gridCurrent[position].startsWith("SB0")
                    or ed.gridCurrent[position].startsWith("SC0"))
                cellCanvas.drawLine(0f, 0f, 0f, (cellCanvas.height - 1).toFloat(), paint) // left

            if (ed.gridCurrent[position].length > 2)
                if ((ed.gridCurrent[position][0] == 'L') or (ed.gridCurrent[position][0] == 'S') and (ed.gridCurrent[position][2] == '2'))
                    cellCanvas.drawLine((cellCanvas.width - 1).toFloat(), 0f, (cellCanvas.width - 1).toFloat(), (cellCanvas.height - 1).toFloat(), paint) // right
        }
        // horizontal (top, bottom)
        if (ed.gridCurrent[position].startsWith("T0")) cellCanvas.drawLine(0f, 0f, (cellCanvas.width - 1).toFloat(), 0f, paint) // top
        if (ed.gridCurrent[position].startsWith("N0")) cellCanvas.drawLine(0f, 0f, (cellCanvas.width - 1).toFloat(), 0f, paint) // top
        if (ed.gridCurrent[position].startsWith("T6")) cellCanvas.drawLine(0f, 0f, (cellCanvas.width - 1).toFloat(), 0f, paint) // top
        if (ed.gridCurrent[position].startsWith("N6")) cellCanvas.drawLine(0f, 0f, (cellCanvas.width - 1).toFloat(), 0f, paint) // top
        if (ed.gridCurrent[position].startsWith("X02")) cellCanvas.drawLine(0f, 0f, (cellCanvas.width - 1).toFloat(), 0f, paint) // top
        if (ed.gridCurrent[position].startsWith("S")) cellCanvas.drawLine(0f, 0f, (cellCanvas.width - 1).toFloat(), 0f, paint) // top
        if (ed.gridCurrent[position].startsWith("X03")) cellCanvas.drawLine(0f, 0f, (cellCanvas.width - 1).toFloat(), 0f, paint) // top

        if (ed.gridCurrent[position].startsWith("X00")) cellCanvas.drawLine(0f, (cellCanvas.height - 1).toFloat(), (cellCanvas.width - 1).toFloat(), (cellCanvas.height - 1).toFloat(), paint) // bottom
        if (ed.gridCurrent[position].startsWith("L")) cellCanvas.drawLine(0f, (cellCanvas.height - 1).toFloat(), (cellCanvas.width - 1).toFloat(), (cellCanvas.height - 1).toFloat(), paint) // bottom
        if (ed.gridCurrent[position].startsWith("X01")) cellCanvas.drawLine(0f, (cellCanvas.height - 1).toFloat(), (cellCanvas.width - 1).toFloat(), (cellCanvas.height - 1).toFloat(), paint) // bottom
        if (ed.gridCurrent[position].startsWith("T5")) cellCanvas.drawLine(0f, (cellCanvas.height - 1).toFloat(), (cellCanvas.width - 1).toFloat(), (cellCanvas.height - 1).toFloat(), paint) // bottom
        if (ed.gridCurrent[position].startsWith("N5")) cellCanvas.drawLine(0f, (cellCanvas.height - 1).toFloat(), (cellCanvas.width - 1).toFloat(), (cellCanvas.height - 1).toFloat(), paint) // bottom
        if (ed.gridCurrent[position].startsWith("T9")) cellCanvas.drawLine(0f, (cellCanvas.height - 1).toFloat(), (cellCanvas.width - 1).toFloat(), (cellCanvas.height - 1).toFloat(), paint) // bottom
        if (ed.gridCurrent[position].startsWith("N9")) cellCanvas.drawLine(0f, (cellCanvas.height - 1).toFloat(), (cellCanvas.width - 1).toFloat(), (cellCanvas.height - 1).toFloat(), paint) // bottom

        // draw circel of beginning playerName
        if ((ed.gridCurrent[position] == "LA0") and (ed.playerStart == 'A')
                or ((ed.gridCurrent[position] == "LB0") and (ed.playerStart == 'B'))
                or ((ed.gridCurrent[position] == "LC0") and (ed.playerStart == 'C'))) {
            paint.style = Paint.Style.FILL
            paint.color = ContextCompat.getColor(context, R.color.colorPlayerStart)
            val x = cellCanvas.width / 8 * 7 - 5
            val y = cellCanvas.height / 8 * 7 - 5
            val radius = cellCanvas.width / 10
            cellCanvas.drawCircle(x.toFloat(), y.toFloat(), radius.toFloat(), paint)
        }
        // item text
        paint.color = Color.BLACK
        paint.textSize = cellCanvas.height * 0.6f
        if (ed.gridCurrent[position].startsWith("S") and (gridValues[position].length > 2))
            paint.textSize = cellCanvas.height * 0.5f
        paint.style = Paint.Style.FILL_AND_STROKE
        paint.strokeWidth = strokeWidthFont.toFloat()
        val bounds = Rect()
        paint.textAlign = Paint.Align.CENTER
        paint.getTextBounds(gridValues[position], 0, gridValues[position].length, bounds)
        val x = cellCanvas.width / 2
        val y = (cellCanvas.height / 2 - (paint.descent() + paint.ascent()) / 2).toInt()
        //Lars, 19.06.2017
        paint.flags = Paint.ANTI_ALIAS_FLAG
        cellCanvas.drawText(gridValues[position], x.toFloat(), y.toFloat(), paint)

        if (isOnline && ed.gridCurrent[position].startsWith("X03")) {
            paint.color = ContextCompat.getColor(context, R.color.colorBackgroundOnline)
            if (isOnlineMatch)
                paint.color = ContextCompat.getColor(context, R.color.colorBackgroundOnlineMatch)
            paint.style = Paint.Style.FILL
            val mRect = RectF(0f, 0f, cellCanvas.width.toFloat(), cellCanvas.height.toFloat())
            cellCanvas.drawRect(mRect, paint)
            val icon = BitmapFactory.decodeResource(resources, R.drawable.icon_firebase).copy(Bitmap.Config.ARGB_8888, true)
            var st = strokeWidthBold
            if (cellWidth < 100)
                st = 0
            cellCanvas.drawBitmap(icon, null, Rect(st, st, cellWidth - st - 1, cellHeight - st - 1), null)
        }

        // draw frame
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = strokeWidth.toFloat()
        paint.color = ContextCompat.getColor(context, R.color.colorTableStroke)
        cellCanvas.drawLine(0f, 0f, 0f, cellCanvas.height.toFloat(), paint) // left
        cellCanvas.drawLine((cellCanvas.width - 1).toFloat(), 0f, (cellCanvas.width - 1).toFloat(), cellCanvas.height.toFloat(), paint) // right
        cellCanvas.drawLine(0f, 0f, cellCanvas.width.toFloat(), 0f, paint) // top
        cellCanvas.drawLine(0f, (cellCanvas.height - 1).toFloat(), (cellCanvas.width - 1).toFloat(), (cellCanvas.height - 1).toFloat(), paint) // bottom

        canvas.drawBitmap(bitmap, cellValues[position][0].toFloat(), cellValues[position][1].toFloat(), mPaint)

    }

    fun updateTable(escaleroData: EscaleroData, isOnline: Boolean, isOnlineMatch: Boolean) {
        ed = escaleroData
        this.isOnline = isOnline
        this.isOnlineMatch = isOnlineMatch
        gridValues = ed.gridValues
        invalidate()
    }

    fun getPositionFromTouchPoint(touchPoint: Point): Int {
        for (i in 0 until cellCnt) {
            val x1 = cellValues[i][0]
            val y1 = cellValues[i][1]
            val x2 = cellValues[i][0] + cellValues[i][2]
            val y2 = cellValues[i][1] + cellValues[i][3]
            val rect = Rect(x1, y1, x2, y2)
            if (rect.contains(touchPoint.x, touchPoint.y))
                return i
        }
        return 0
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val widthMode = MeasureSpec.getMode(widthMeasureSpec)
        val widthSize = MeasureSpec.getSize(widthMeasureSpec)
        val heightMode = MeasureSpec.getMode(heightMeasureSpec)
        val heightSize = MeasureSpec.getSize(heightMeasureSpec)
        val desiredHeight: Int

        val desiredWidth =
            if (widthSize < heightSize)
                widthSize
            else
                heightSize / 3 * 4 // aspect ratio 4:3
        var width = when {
            (widthMode == MeasureSpec.EXACTLY)
                -> widthSize
            (widthMode == MeasureSpec.AT_MOST)
                -> min(desiredWidth, widthSize)
            else
                -> desiredWidth
        }

        desiredHeight = width // aspect ratio 4:3

        var height = when {
            (heightMode == MeasureSpec.EXACTLY)
                -> heightSize
            (heightMode == MeasureSpec.AT_MOST)
                -> min(desiredHeight, heightSize)
            else
                -> desiredHeight
        }

        //Log.i(TAG, "1 onMeasure(), displayWidth: " + displayWidth + ", displayHeight: " + displayHeight + ", orientation: " + orientation);

        val maxLandscapeWidth = (displayWidth * tableLandscapeWidth).toInt()
        val maxPortraitHeight = (displayHeight * tablePortraitHeight).toInt()
        if ((orientation == Configuration.ORIENTATION_PORTRAIT) and (height > maxPortraitHeight)) {
            width = displayWidth
            height = maxPortraitHeight
        }
        if ((orientation == Configuration.ORIENTATION_LANDSCAPE) and (width > maxLandscapeWidth)) {
            width = maxLandscapeWidth
            height = displayHeight
        }

        //Log.i(TAG, "2 onMeasure(), width: " + width + ", height: " + height + ", maxW: " + maxLandscapeWidth + ", maxH: " + maxPortraitHeight);

        //MUST CALL THIS
        setMeasuredDimension(width, height)

    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        if ((w > 0) and (h > 0)) {
            tableWidth = w
            tableHeight = h
            cellWidth = tableWidth / tableCol
            cellHeight = tableHeight / tableRow
            strokeWidth = 1
            strokeWidthBold = 2
            //            int st = ((cellWidth + cellHeight) / 2) / 50;
            val st = (cellWidth + cellHeight) / 100
            if (st > 1) {
                strokeWidth = st
                strokeWidthBold = st * 3
            }
            //Log.i(TAG, "onSizeChanged(), tableW: " + tableWidth + ", tableH: " + tableHeight + ", cellW: " + cellWidth + ", cellH: " + cellHeight + ", strokeW: " + strokeWidth);
            initCellValues()
        }
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }
    companion object {
        const val TAG = "TableView"
        const val VALUES = 4   // x,y,cellWidth,cellHeight
    }

}
