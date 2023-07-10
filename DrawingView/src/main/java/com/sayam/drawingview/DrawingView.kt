package com.sayam.drawingview

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View
import com.sayam.drawingview.model.*


typealias  funUndoRedo = (isUndoVisible: Boolean, isRedoVisible: Boolean) -> Unit

class DrawingView(context: Context, attrs: AttributeSet) : View(context, attrs) {

    private val allShapeRedo: ArrayList<Shape> = ArrayList()                // redo
    private val allShape: ArrayList<Shape> = ArrayList()                    // undo

    private val toolsData: ArrayList<ToolProperties> = ArrayList()
    private val tempPath = Path()                   // this path is used by triangle, heart
    private var tempData = 0f                       // used to store temp value for some drawing
    private val tempVector = Vector2()              // used to store temp value for some drawing
    private var isCurrentShapeDrawing = false
    private var currentDrawingShape = Shape()
    private var currentSelectedTool = ShapeType.Rectangle
    private var isRedrawAllowed = true
    private var canvasPosition = Vector2()          // store canvas translate position
    private val whiteBoardRect = Rect()
    private var previousTouch = Vector2()
    private var projectSavedBitmap: Bitmap? = null
    private var whiteBoardPaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.FILL
        color = Color.WHITE
    }
    private var shapePaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.STROKE
        color = Color.BLACK
        strokeWidth = 4f
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND
    }

    // store drawing limit of canvas, out off this offset shape will not be drawn
    private val canvasDrawingAreaOffset = 40.toPx
    private val canvasWhiteBoardOffsetRect = Rect()

    // Note: Update needed
    private var catchBitmap: Bitmap? = null
    private lateinit var canvasBitmap: Canvas
    private var totalShapeDrawn = 0
    private var undoRedoFun: funUndoRedo? = null
    private var isProjectSaved = true

    fun setUndoRedoListener(callback: funUndoRedo) {
        undoRedoFun = callback
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.translate(canvasPosition.x, canvasPosition.y)
        canvas.clipRect(whiteBoardRect)

        // this will draw white screen if the project is new.
        // Otherwise it will draw saved project bitmap.
        if (projectSavedBitmap == null) canvas.drawRect(
            whiteBoardRect,
            whiteBoardPaint
        ) else canvas.drawBitmap(projectSavedBitmap!!, 0F, 0F, null)

        if (isRedrawAllowed) {

            undoRedoFun?.invoke(allShape.isNotEmpty(), allShapeRedo.isNotEmpty())
            isRedrawAllowed = false
        } else if (totalShapeDrawn < allShape.size) {

            undoRedoFun?.invoke(allShape.isNotEmpty(), allShapeRedo.isNotEmpty())

            if (catchBitmap != null) onDrawBitmap(canvasBitmap)
        }

        if (catchBitmap != null) canvas.drawBitmap(catchBitmap!!, 0F, 0F, null)

        // drawing current drawing shape
        if (isCurrentShapeDrawing) {
            drawShape(canvas, currentDrawingShape)
        }

    }

    private fun onDrawBitmap(canvas: Canvas, isDrawAll: Boolean = false) {

        if (isDrawAll) {
            catchBitmap!!.eraseColor(Color.TRANSPARENT)
            for (shape in allShape) {
                drawShape(canvas, shape)
            }
        } else drawShape(canvas, allShape.last())
        totalShapeDrawn = allShape.size
    }

    private fun drawShape(canvas: Canvas, shape: Shape) {

        when (shape.shapeType) {

            ShapeType.Rectangle -> {
                shape as Rectangle

                // drawing fill shape if color is not transparent
                if (!isColorTransparent(shape.fillColor)) {
                    shapePaint.color = shape.fillColor
                    shapePaint.style = Paint.Style.FILL
                    canvas.drawRect(
                        shape.startPos.x,
                        shape.startPos.y,
                        shape.endPos.x,
                        shape.endPos.y,
                        shapePaint
                    )
                }

                // drawing stroke shape if color is not transparent
                if (!isColorTransparent(shape.strokeColor)) {
                    shapePaint.color = shape.strokeColor
                    shapePaint.strokeWidth = shape.strokeWidth
                    shapePaint.style = Paint.Style.STROKE
                    canvas.drawRect(
                        shape.startPos.x,
                        shape.startPos.y,
                        shape.endPos.x,
                        shape.endPos.y, shapePaint
                    )
                }

            }

            ShapeType.Brush -> {
                shape as Brush
                // drawing stroke if color is not transparent
                if (!isColorTransparent(shape.strokeColor)) {

                    shapePaint.color = shape.strokeColor
                    shapePaint.strokeWidth = shape.strokeWidth
                    shapePaint.style = Paint.Style.STROKE
                    canvas.drawPath(shape.path, shapePaint)
                }
            }

            ShapeType.Eraser -> {
                shape as Eraser

                Log.e("======", "hello")

                shapePaint.color = shape.strokeColor
                shapePaint.strokeWidth = shape.strokeWidth
                shapePaint.style = Paint.Style.STROKE
                canvas.drawPath(shape.path, shapePaint)
            }

            ShapeType.Line -> {
                shape as Line
                // drawing stroke if color is not transparent
                if (!isColorTransparent(shape.strokeColor)) {

                    shapePaint.color = shape.strokeColor
                    shapePaint.strokeWidth = shape.strokeWidth
                    shapePaint.style = Paint.Style.STROKE
                    canvas.drawLine(
                        shape.startPos.x,
                        shape.startPos.y,
                        shape.endPos.x,
                        shape.endPos.y,
                        shapePaint
                    )
                }
            }

            ShapeType.Oval -> {
                shape as Oval

                // drawing fill shape if color is not transparent
                if (!isColorTransparent(shape.fillColor)) {
                    shapePaint.color = shape.fillColor
                    shapePaint.style = Paint.Style.FILL
                    canvas.drawOval(
                        shape.startPos.x,
                        shape.startPos.y,
                        shape.endPos.x,
                        shape.endPos.y,
                        shapePaint
                    )
                }

                // drawing stroke if color is not transparent
                if (!isColorTransparent(shape.strokeColor)) {

                    shapePaint.color = shape.strokeColor
                    shapePaint.strokeWidth = shape.strokeWidth
                    shapePaint.style = Paint.Style.STROKE
                    canvas.drawOval(
                        shape.startPos.x,
                        shape.startPos.y,
                        shape.endPos.x,
                        shape.endPos.y,
                        shapePaint
                    )
                }

            }

            ShapeType.Triangle -> {
                shape as Triangle

                // Note:   (shape.endPos.x - shape.startPos.x) / 2 + shape.startPos.x is used to
                //         calculate mid-x point in rect

                tempPath.reset()        // clear path for drawing
                tempPath.moveTo(
                    (shape.endPos.x - shape.startPos.x) / 2 + shape.startPos.x,
                    shape.startPos.y
                )
                tempPath.lineTo(shape.endPos.x, shape.endPos.y)
                tempPath.lineTo(shape.startPos.x, shape.endPos.y)
                tempPath.close()

                // drawing fill if color is not transparent
                if (!isColorTransparent(shape.fillColor)) {

                    shapePaint.color = shape.fillColor
                    shapePaint.style = Paint.Style.FILL
                    canvas.drawPath(tempPath, shapePaint)
                }

                // drawing stroke if color is not transparent
                if (!isColorTransparent(shape.strokeColor)) {

                    shapePaint.color = shape.strokeColor
                    shapePaint.strokeWidth = shape.strokeWidth
                    shapePaint.style = Paint.Style.STROKE
                    canvas.drawPath(tempPath, shapePaint)
                }

            }

            ShapeType.Heart -> {
                shape as Heart
                tempPath.reset()        // clear path for drawing

                tempVector.setValue(shape.endPos.minus(shape.startPos))   // difference of two vector
                // calculating heart top center position
                tempPath.moveTo(
                    shape.startPos.x + tempVector.x / 2,
                    shape.startPos.y + tempVector.y * 0.3f
                )

                // adding left part of heart
                tempPath.cubicTo(
                    shape.startPos.x + tempVector.x * 0.3f,
                    shape.startPos.y,
                    shape.startPos.x,
                    shape.startPos.y + tempVector.y * 0.5f,
                    shape.startPos.x + tempVector.x * 0.5f,
                    shape.endPos.y
                )

                // calculating heart top center position
                tempPath.moveTo(
                    shape.startPos.x + tempVector.x / 2,
                    shape.startPos.y + tempVector.y * 0.3f
                )

                // adding right part of heart
                tempPath.cubicTo(
                    shape.endPos.x - tempVector.x * 0.3f,
                    shape.startPos.y,
                    shape.endPos.x,
                    shape.startPos.y + tempVector.y * 0.5f,
                    shape.endPos.x - tempVector.x * 0.5f,
                    shape.endPos.y
                )

                // drawing fill if color is not transparent
                if (!isColorTransparent(shape.fillColor)) {

                    shapePaint.color = shape.fillColor
                    shapePaint.style = Paint.Style.FILL
                    canvas.drawPath(tempPath, shapePaint)
                }

                // drawing stroke if color is not transparent
                if (!isColorTransparent(shape.strokeColor)) {

                    shapePaint.color = shape.strokeColor
                    shapePaint.strokeWidth = shape.strokeWidth
                    shapePaint.style = Paint.Style.STROKE
                    canvas.drawPath(tempPath, shapePaint)
                }

            }

            else -> {}
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {

        val touchPos = Vector2(event.x, event.y)

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {

                // if touch will not in limiting area then skip this step
                if (!isTouchWithinLimitingArea(touchPos)) return false

                val selectedTool = getSelectedToolProp(currentSelectedTool)
                if (selectedTool != null) {

                    previousTouch = touchPos
                    isCurrentShapeDrawing = true

                    when (currentSelectedTool) {
                        ShapeType.Brush -> {
                            currentDrawingShape = Brush()
                            currentDrawingShape.strokeColor = selectedTool.strokeColor
                            currentDrawingShape.strokeWidth = selectedTool.strokeWidth

                            val pos = screenToCanvas(touchPos)
                            (currentDrawingShape as Brush).path.moveTo(pos.x, pos.y)
                            (currentDrawingShape as Brush).path.lineTo(pos.x, pos.y)
                            //(currentDrawingShape as Brush).addMoveTo(pos.x, pos.y)

                        }
                        ShapeType.Eraser -> {
                            currentDrawingShape = Eraser()
                            currentDrawingShape.strokeColor = selectedTool.strokeColor
                            currentDrawingShape.strokeWidth = selectedTool.strokeWidth

                            val pos = screenToCanvas(touchPos)
                            (currentDrawingShape as Eraser).path.moveTo(pos.x, pos.y)
                            (currentDrawingShape as Eraser).path.lineTo(pos.x, pos.y)
                            //(currentDrawingShape as Eraser).addMoveTo(pos.x, pos.y)

                        }
                        ShapeType.Rectangle -> {
                            currentDrawingShape = Rectangle()
                            currentDrawingShape.fillColor = selectedTool.fillColor
                            currentDrawingShape.strokeColor = selectedTool.strokeColor
                            currentDrawingShape.strokeWidth = selectedTool.strokeWidth
                            (currentDrawingShape as Rectangle).startPos.setValue(
                                screenToCanvas(
                                    touchPos
                                )
                            )
                            (currentDrawingShape as Rectangle).endPos.setValue(
                                screenToCanvas(
                                    touchPos
                                )
                            )

                        }
                        ShapeType.Line -> {
                            currentDrawingShape = Line()
                            currentDrawingShape.strokeColor = selectedTool.strokeColor
                            currentDrawingShape.strokeWidth = selectedTool.strokeWidth
                            (currentDrawingShape as Line).startPos.setValue(screenToCanvas(touchPos))
                            (currentDrawingShape as Line).endPos.setValue(screenToCanvas(touchPos))

                        }
                        ShapeType.Oval -> {
                            currentDrawingShape = Oval()
                            currentDrawingShape.fillColor = selectedTool.fillColor
                            currentDrawingShape.strokeColor = selectedTool.strokeColor
                            currentDrawingShape.strokeWidth = selectedTool.strokeWidth
                            (currentDrawingShape as Oval).startPos.setValue(screenToCanvas(touchPos))
                            (currentDrawingShape as Oval).endPos.setValue(screenToCanvas(touchPos))

                        }
                        ShapeType.Triangle -> {
                            currentDrawingShape = Triangle()
                            currentDrawingShape.fillColor = selectedTool.fillColor
                            currentDrawingShape.strokeColor = selectedTool.strokeColor
                            currentDrawingShape.strokeWidth = selectedTool.strokeWidth
                            (currentDrawingShape as Triangle).startPos.setValue(
                                screenToCanvas(
                                    touchPos
                                )
                            )
                            (currentDrawingShape as Triangle).endPos.setValue(
                                screenToCanvas(
                                    touchPos
                                )
                            )
                        }
                        ShapeType.Heart -> {
                            currentDrawingShape = Heart()
                            currentDrawingShape.fillColor = selectedTool.fillColor
                            currentDrawingShape.strokeColor = selectedTool.strokeColor
                            currentDrawingShape.strokeWidth = selectedTool.strokeWidth
                            (currentDrawingShape as Heart).startPos.setValue(screenToCanvas(touchPos))
                            (currentDrawingShape as Heart).endPos.setValue(screenToCanvas(touchPos))
                        }

                        else -> {}
                    }

                } else {
                    return false
                }
            }

            MotionEvent.ACTION_POINTER_DOWN -> {

                // this callback is only for zoom and pan
                if (event.pointerCount > 1) {
                    if (isCurrentShapeDrawing) isCurrentShapeDrawing = false
                    val touch1 = Vector2(event.getX(0), event.getY(0))
                    val touch2 = Vector2(event.getX(1), event.getY(1))
                    previousTouch = touch1.minPointVector(touch2)
                    invalidate()

                }
            }

            MotionEvent.ACTION_MOVE -> {

                // this callback is only for zoom and pan
                if (event.pointerCount > 1) {
                    if (isCurrentShapeDrawing) isCurrentShapeDrawing = false
                    val touch1 = Vector2(event.getX(0), event.getY(0))
                    val touch2 = Vector2(event.getX(1), event.getY(1))
                    val currentTouch = touch1.minPointVector(touch2)
                    val deltaChange = currentTouch - previousTouch
                    canvasPosition += deltaChange
                    previousTouch = currentTouch

                    invalidate()
                    return true
                }

                // this will allow or disallow drawing new shapes
                if (isCurrentShapeDrawing) {

                    when (currentDrawingShape.shapeType) {
                        ShapeType.Brush -> {
                            val ctp = screenToCanvas(previousTouch)
                            val endP = screenToCanvas(touchPos)
                            val newP = Vector2((endP.x + ctp.x) / 2, (endP.y + ctp.y) / 2)
                            (currentDrawingShape as Brush).path.quadTo(ctp.x, ctp.y, newP.x, newP.y)
                            (currentDrawingShape as Brush).addQuadTo(ctp.x, ctp.y, newP.x, newP.y)
                            previousTouch.setValue(touchPos)

                        }
                        ShapeType.Eraser -> {
                            val ctp = screenToCanvas(previousTouch)
                            val endP = screenToCanvas(touchPos)
                            val newP = Vector2((endP.x + ctp.x) / 2, (endP.y + ctp.y) / 2)
                            (currentDrawingShape as Eraser).path.quadTo(
                                ctp.x,
                                ctp.y,
                                newP.x,
                                newP.y
                            )
                            (currentDrawingShape as Eraser).addQuadTo(ctp.x, ctp.y, newP.x, newP.y)
                            previousTouch.setValue(touchPos)

                        }
                        ShapeType.Rectangle -> {
                            (currentDrawingShape as Rectangle).endPos.setValue(
                                screenToCanvas(
                                    touchPos
                                )
                            )

                        }
                        ShapeType.Line -> {
                            (currentDrawingShape as Line).endPos.setValue(screenToCanvas(touchPos))

                        }
                        ShapeType.Oval -> {
                            (currentDrawingShape as Oval).endPos.setValue(screenToCanvas(touchPos))

                        }
                        ShapeType.Triangle -> {
                            (currentDrawingShape as Triangle).endPos.setValue(
                                screenToCanvas(
                                    touchPos
                                )
                            )
                        }
                        ShapeType.Heart -> {
                            (currentDrawingShape as Heart).endPos.setValue(screenToCanvas(touchPos))
                        }

                        else -> {}
                    }

                    invalidate()
                }
            }

            MotionEvent.ACTION_UP -> {
                if (isCurrentShapeDrawing) {

                    if (isProjectSaved) isProjectSaved = false

                    allShape.add(currentDrawingShape)
                    if (allShapeRedo.isNotEmpty()) allShapeRedo.clear()
                    isCurrentShapeDrawing = false
                    invalidate()
                }
            }

            else -> return false

        }

        return true
    }

    private fun isTouchWithinLimitingArea(vector2: Vector2): Boolean {
        val pos = screenToCanvas(vector2)
        if (pos.x > canvasWhiteBoardOffsetRect.left && pos.x < canvasWhiteBoardOffsetRect.right && pos.y > canvasWhiteBoardOffsetRect.top && pos.y < canvasWhiteBoardOffsetRect.bottom) {
            return true
        }
        return false
    }


    fun getSelectedToolProp(selectedTool: ShapeType): ToolProperties? {
        for (tool in toolsData) {
            if (selectedTool == tool.shapeType) {
                return tool
            }
        }

        return null
    }

    private fun screenToCanvas(screenPos: Vector2): Vector2 {
        return screenPos - canvasPosition
    }


    private fun isColorTransparent(color: Int): Boolean {
        val alpha = color shr 24 and 0xff
        return alpha == 0
    }

    fun getCanvasBitmap(): Bitmap {
        val bitmap = Bitmap.createBitmap(
            whiteBoardRect.width(),
            whiteBoardRect.height(),
            Bitmap.Config.ARGB_8888
        )
        val bitmapCanvas = Canvas(bitmap)

        //bitmapCanvas.clipRect(whiteBoardRect)

        if (projectSavedBitmap == null) bitmapCanvas.drawRect(
            whiteBoardRect,
            whiteBoardPaint
        ) else {
            bitmapCanvas.drawBitmap(projectSavedBitmap!!, 0f, 0f, null)
        }
        for (shape in allShape) drawShape(bitmapCanvas, shape)
        return bitmap
    }


    // the below functions will be called from fragment or activity

    fun getToolData(): ArrayList<ToolProperties> {
        return toolsData
    }

    fun setToolData(data: ArrayList<ToolProperties>) {
        toolsData.clear()
        for (prop in data) {
            toolsData.add(prop)
        }
    }

    fun setProjectSavedBitmap(bitmap: Bitmap?) {
        if (bitmap == null) return
        projectSavedBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        invalidate()
    }

    fun updateToolData(tempToolData: ToolProperties) {
        for (index in 0..toolsData.lastIndex) {
            if (tempToolData.shapeType == toolsData[index].shapeType) {

                // updating
                toolsData[index] = tempToolData
                return
            }
        }
    }

    fun setSelectedTool(toolType: ShapeType) {
        currentSelectedTool = toolType
    }

    fun setWhiteBoardSize(width: Int, height: Int) {
        // note this function is calling after some delay
        whiteBoardRect.right = width
        whiteBoardRect.bottom = height

        // calculating canvas drawing limit offset. This will stop drawing if touch is out of bound of this area
        canvasWhiteBoardOffsetRect.left = -canvasDrawingAreaOffset
        canvasWhiteBoardOffsetRect.top = -canvasDrawingAreaOffset
        canvasWhiteBoardOffsetRect.right = whiteBoardRect.right + canvasDrawingAreaOffset
        canvasWhiteBoardOffsetRect.bottom = whiteBoardRect.bottom + canvasDrawingAreaOffset

        // creating bitmap for drawing, It size will be fixed once created
        catchBitmap = Bitmap.createBitmap(
            whiteBoardRect.width(),
            whiteBoardRect.height(),
            Bitmap.Config.ARGB_8888
        )
        canvasBitmap = Canvas(catchBitmap!!)

        // calculating default translate
        val top = measuredHeight / 2f - height / 2
        val left = 0f
        canvasPosition.setValue(left, top)

    }

    fun undo() {
        if (allShape.size == 0) return
        allShapeRedo.add(allShape.removeLast())
    }

    fun redo() {
        if (allShapeRedo.size == 0) return
        allShape.add(allShapeRedo.removeLast())
    }

    fun clearCanvas() {
        if (projectSavedBitmap == null) return
        allShape.clear()
        allShapeRedo.clear()
        projectSavedBitmap!!.eraseColor(Color.WHITE)
        onDrawBitmap(canvasBitmap, true)
        isProjectSaved = false
        isRedrawAllowed = true
        invalidate()
    }

    suspend fun reDraw() {
        // creating bitmap
        onDrawBitmap(canvasBitmap, true)
        isRedrawAllowed = true
    }

    fun setProjectSaved() {
        isProjectSaved = true
    }

    fun isProjectSaved(): Boolean {
        return isProjectSaved
    }
}