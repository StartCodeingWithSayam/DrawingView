package com.sayam.drawingview.model

import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.util.Size
import android.util.TypedValue
import android.view.View
import androidx.core.graphics.PathParser


data class Vector2(var x: Float = 0f, var y: Float = 0f) {
    operator fun minus(vector: Vector2): Vector2 {
        return Vector2(this.x - vector.x, this.y - vector.y)
    }

    fun minPointVector(vector: Vector2): Vector2{
        return Vector2(this.x - (this.x - vector.x)/2, this.y - (this.y - vector.y)/2)
    }

    fun addOffset(x: Float, y: Float){
        this.x += x
        this.y += y
    }

    operator fun plus(vector: Vector2): Vector2 {
        return Vector2(this.x + vector.x, this.y + vector.y)
    }

    fun setValue(vector: Vector2){
        this.x = vector.x
        this.y = vector.y
    }

    fun setValue(x: Float, y:Float){
        this.x = x
        this.y = y
    }
    fun copy(): Vector2{
        return Vector2(this.x, this.y)
    }


}

data class Size(var width: Int = 0, var height: Int = 0)






// -------------------- canvas shape ---------------------------
open class Shape{
    var shapeType = ShapeType.Rectangle
    var fillColor: Int = Color.RED
    var strokeColor: Int = Color.BLACK
    var strokeWidth: Float = 2f
}

class Rectangle: Shape() {
    var startPos = Vector2()
    var endPos = Vector2()
    init {
        shapeType = ShapeType.Rectangle
    }
}


class Line: Shape() {
    var startPos = Vector2()
    var endPos = Vector2()
    init {
        shapeType = ShapeType.Line
    }
}

class Oval: Shape() {
    var startPos = Vector2()
    var endPos = Vector2()
    init {
        shapeType = ShapeType.Oval
    }
}

class Triangle: Shape() {
    var startPos = Vector2()
    var endPos = Vector2()
    init {
        shapeType = ShapeType.Triangle
    }
}

class Heart: Shape() {
    var startPos = Vector2()
    var endPos = Vector2()
    init {
        shapeType = ShapeType.Heart
    }
}



class Brush: Shape(){
    var path = Path()
    var pathString = "M"
    init {
        shapeType = ShapeType.Brush
    }

    fun addMoveTo(startX: Float, startY: Float){
        pathString += "$startX,$startY"

    }
    fun addQuadTo(cx: Float, cy: Float, endX: Float, endY: Float){
        pathString += " Q$cx,$cy $endX,$endY"
    }

    fun toBrushRaw(): BrushRaw{
        val brushRaw = BrushRaw()
        brushRaw.pathString = pathString
        return brushRaw
    }
}

class BrushRaw: Shape(){
    var pathString = ""
    init {
        shapeType = ShapeType.Brush
    }

    fun toBrush(): Brush{
        val brush = Brush()
        brush.path = PathParser.createPathFromPathData(pathString)
        brush.pathString = pathString
        return brush
    }
}


class Eraser: Shape(){
    var path = Path()
    var pathString = "M"
    init {
        shapeType = ShapeType.Eraser
        strokeColor = Color.WHITE
    }

    fun addMoveTo(startX: Float, startY: Float){
        pathString += "$startX,$startY"

    }
    fun addQuadTo(cx: Float, cy: Float, endX: Float, endY: Float){
        pathString += " Q$cx,$cy $endX,$endY"
    }

    fun toEraserRaw(): EraserRaw{
        val eraserRaw = EraserRaw()
        eraserRaw.pathString = pathString
        eraserRaw.strokeWidth = strokeWidth
        return eraserRaw
    }
}

class EraserRaw: Shape(){
    var pathString = ""
    init {
        shapeType = ShapeType.Eraser
        strokeColor = Color.WHITE
    }

    fun toEraser(): Eraser{
        val eraser = Eraser()
        eraser.path = PathParser.createPathFromPathData(pathString)
        eraser.strokeWidth = strokeWidth
        eraser.strokeColor = strokeColor
        eraser.pathString = pathString
        return eraser
    }
}




data class ToolProperties(
    var shapeType: ShapeType = ShapeType.Rectangle,
    var fillColor: Int = Color.RED,
    var strokeColor: Int = Color.BLACK,
    var strokeWidth: Float = 4f
)

enum class ShapeType{
    Rectangle,
    Oval,
    Line,
    Brush,
    Eraser,
    Triangle,
    Heart,
}



object Utils {
    fun setWrapSize( widthInPixel:Int,heightInPixel:Int,widthMeasureSpec: Int, heightMeasureSpec: Int): Size {
        val widthMode = View.MeasureSpec.getMode(widthMeasureSpec)
        val widthSize = View.MeasureSpec.getSize(widthMeasureSpec)
        val heightMode = View.MeasureSpec.getMode(heightMeasureSpec)
        val heightSize = View.MeasureSpec.getSize(heightMeasureSpec)
        //Measure Width
        val width = when (widthMode) {
            View.MeasureSpec.EXACTLY -> {
                //Must be this size
                widthSize
            }
            View.MeasureSpec.AT_MOST -> {
                //Can't be bigger than...
                Math.min(widthInPixel, widthSize)
            }
            else -> {
                //Be whatever you want
                widthInPixel
            }
        }
        //Measure Height
        val height = when (heightMode) {
            View.MeasureSpec.EXACTLY -> {
                //Must be this size
                heightSize
            }
            View.MeasureSpec.AT_MOST -> {
                //Can't be bigger than...
                Math.min(heightInPixel, heightSize)
            }
            else -> {
                //Be whatever you want
                heightInPixel
            }
        }
        return Size(width,height)
    }
    fun fillPaint(color:Int= Color.BLACK): Paint = Paint().apply {
        style= Paint.Style.FILL
        this.color=color
    }
    fun strokePaint(color:Int= Color.BLACK): Paint = Paint().apply {
        style= Paint.Style.STROKE
        this.color=color
    }
    fun toRGB(colorWithAlpha: Int):Int{
        return Color.rgb(Color.red(colorWithAlpha), Color.green(colorWithAlpha), Color.blue(colorWithAlpha))
    }
    fun colorWithAlpha(a:Int,color: Int):Int{
        return Color.argb(a,Color.red(color), Color.green(color), Color.blue(color))
    }
}
fun IntArray.swapFromBack() {
    for (i in (size - 1) downTo 1) {
        this[i] = this[i - 1]
    }
}

fun View.getBitmap(): Bitmap {
    val bitmap = Bitmap.createBitmap(measuredWidth, measuredHeight, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    draw(canvas)
    return bitmap
}

fun RectF.fromXYWH(x: Float, y: Float, w: Float, h: Float): RectF {
    left = x
    top = y
    right = x + w
    bottom = y + h
    return this
}

fun Int.toHSV(): FloatArray {
    val hsv = FloatArray(3)
    Color.colorToHSV(this, hsv)
    return hsv
}
fun View.getRectF() = RectF().fromXYWH(0f, 0f, measuredWidth.toFloat(), measuredHeight.toFloat())
val Number.toPx get() = TypedValue.applyDimension(
    TypedValue.COMPLEX_UNIT_DIP,
    this.toFloat(),
    Resources.getSystem().displayMetrics).toInt()